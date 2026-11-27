package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.AuthResponseDTO;
import com.lucasbarros.nitrate.dto.LoginRequestDTO;
import com.lucasbarros.nitrate.dto.RegisterRequestDTO;
import com.lucasbarros.nitrate.entities.RevokedToken;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.RevokedTokenRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.security.JwtService;
import com.lucasbarros.nitrate.security.RateLimiter;
import com.lucasbarros.nitrate.services.exceptions.EmailJaCadastradoException;
import com.lucasbarros.nitrate.services.exceptions.TooManyRequestsException;
import com.lucasbarros.nitrate.services.exceptions.UsernameJaCadastradoException;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,20}$");
    // nomes que são rotas de /users/... e não podem virar perfil, e o da conta das listas oficiais
    private static final Set<String> USERNAMES_RESERVADOS = Set.of("me", "search", NitrateAccountService.USERNAME);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int NOME_MAX = 60; // igual ao AccountService
    private static final int EMAIL_MAX = 254;
    private static final int SENHA_MIN = 8;
    private static final int SENHA_MAX_BYTES = 72; // o bcrypt ignora o que passar disso
    private static final int IDADE_MINIMA = 13; // igual aos Termos de Uso do front (MIN_AGE)

    // força bruta numa conta: senhas erradas seguidas bloqueiam o login dela a partir daquele IP por
    // um tempo. Bloquear a conta inteira deixaria qualquer um trancar a conta de outra pessoa errando
    // a senha de propósito; o teto da conta, bem mais alto, segura quem tenta de muitos IPs
    // (o limite geral por IP fica no RateLimitFilter)
    private static final int FALHAS_MAX_POR_IP = 10;
    private static final int FALHAS_MAX_CONTA = 50;
    private static final Duration JANELA_FALHAS = Duration.ofMinutes(15);

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private RateLimiter rateLimiter;
    @Autowired private RevokedTokenRepository revokedTokenRepository;

    public AuthResponseDTO registrar(RegisterRequestDTO request) {
        String nome = request.getName() == null ? "" : request.getName().trim();
        String username = request.getUsername() == null ? "" : request.getUsername().trim();
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase(Locale.ROOT);
        String senha = request.getPassword() == null ? "" : request.getPassword();

        if (nome.isEmpty()) throw new IllegalArgumentException("Informe seu nome.");
        if (nome.length() > NOME_MAX) throw new IllegalArgumentException("O nome pode ter até " + NOME_MAX + " caracteres.");

        validarUsername(username);

        validarEmail(email);

        validarSenha(senha);

        if (!Boolean.TRUE.equals(request.getAcceptedTerms())) {
            throw new IllegalArgumentException(
                    "Confirme que tem pelo menos " + IDADE_MINIMA + " anos e aceite os Termos de Uso e a Política de Privacidade.");
        }

        // o e-mail da conta das listas oficiais fica reservado, mesmo antes de ela existir
        if (userRepository.existsByEmailIgnoreCase(email) || email.equals(NitrateAccountService.EMAIL)) {
            throw new EmailJaCadastradoException(email);
        }

        if (usernameReservado(username) || userRepository.existsByUsernameIgnoreCase(username)) {
            throw new UsernameJaCadastradoException(username);
        }

        String senhaCriptografada = passwordEncoder.encode(senha);
        User user = new User(nome, username, email, senhaCriptografada);
        user.setTermsAcceptedAt(Instant.now());
        user = userRepository.save(user);

        String token = jwtService.gerarToken(user);
        return new AuthResponseDTO(token, user.getName(), user.getUsername(), user.getEmail());
    }

    public AuthResponseDTO login(LoginRequestDTO request, String ip) {
        String login = request.getLogin() == null ? "" : request.getLogin().trim();
        Optional<User> encontrado = userRepository.findFirstByEmailIgnoreCase(login)
                .or(() -> userRepository.findFirstByUsernameIgnoreCase(login));
        // pela conta, e não pelo que foi digitado: o e-mail e o @ dividem as mesmas tentativas
        String chaveConta = chaveFalhas(encontrado.map(u -> "u" + u.getId()).orElse("l" + login.toLowerCase(Locale.ROOT)));
        String chaveIp = chaveConta + ip;
        if (rateLimiter.esgotado(chaveIp, FALHAS_MAX_POR_IP) || rateLimiter.esgotado(chaveConta, FALHAS_MAX_CONTA)) {
            throw new TooManyRequestsException("Muitas tentativas de login nesta conta. Espere alguns minutos e tente de novo.");
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(login, request.getPassword()));
        } catch (BadCredentialsException e) {
            rateLimiter.registrar(chaveIp, JANELA_FALHAS);
            rateLimiter.registrar(chaveConta, JANELA_FALHAS);
            throw e;
        }
        rateLimiter.zerar(chaveIp);

        User user = encontrado.orElseThrow();
        String token = jwtService.gerarToken(user, Boolean.TRUE.equals(request.getRememberMe()));
        return new AuthResponseDTO(token, user.getName(), user.getUsername(), user.getEmail());
    }

    /** prefixo das falhas de login de uma conta (de todos os IPs); a redefinição de senha zera todas */
    static String chaveFalhas(String conta) {
        return "login-falhas:" + conta + ":";
    }

    static String chaveFalhas(User user) {
        return chaveFalhas("u" + user.getId());
    }

    /**
     * Logout: o token deixa de valer agora, e não só quando vencer. Token inválido ou já vencido
     * não precisa de nada.
     */
    @Transactional
    public void sair(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) return;
        JwtService.Sessao sessao;
        try {
            sessao = jwtService.ler(authorization.substring(7));
        } catch (JwtException | IllegalArgumentException e) {
            return;
        }
        Instant agora = Instant.now();
        revokedTokenRepository.deleteAllExpiredBefore(agora);
        if (sessao.expiraEm().isAfter(agora)) {
            revokedTokenRepository.save(new RevokedToken(sessao.id(), sessao.expiraEm()));
        }
    }

    /** formato do @, também usado ao trocar o @ no perfil (AccountService) */
    static void validarUsername(String username) {
        if (username == null || !USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException(
                    "Nome de usuário deve ter entre 3 e 20 caracteres, apenas letras, números e _");
        }
    }

    static boolean usernameReservado(String username) {
        return USERNAMES_RESERVADOS.contains(username.toLowerCase(Locale.ROOT));
    }

    /** formato do e-mail (já em minúsculas), também usado ao trocar o e-mail (AccountService) */
    static void validarEmail(String email) {
        if (email == null || email.length() > EMAIL_MAX || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Informe um e-mail válido.");
        }
    }

    /** regras de senha do cadastro, também usadas na redefinição e na troca de senha */
    static void validarSenha(String senha) {
        if (senha == null || senha.length() < SENHA_MIN) {
            throw new IllegalArgumentException("A senha precisa ter pelo menos " + SENHA_MIN + " caracteres.");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > SENHA_MAX_BYTES) {
            throw new IllegalArgumentException("A senha é longa demais.");
        }
    }

    public boolean usernameDisponivel(String username) {
        String limpo = username.trim();
        return !usernameReservado(limpo) && !userRepository.existsByUsernameIgnoreCase(limpo);
    }
}