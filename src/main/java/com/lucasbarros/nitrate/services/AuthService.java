package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.AuthResponseDTO;
import com.lucasbarros.nitrate.dto.LoginRequestDTO;
import com.lucasbarros.nitrate.dto.RegisterRequestDTO;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.security.CustomUserDetails;
import com.lucasbarros.nitrate.security.JwtService;
import com.lucasbarros.nitrate.security.RateLimiter;
import com.lucasbarros.nitrate.services.exceptions.EmailJaCadastradoException;
import com.lucasbarros.nitrate.services.exceptions.TooManyRequestsException;
import com.lucasbarros.nitrate.services.exceptions.UsernameJaCadastradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-z0-9_]{3,20}$");
    // nomes que são rotas de /users/... e não podem virar perfil
    private static final Set<String> USERNAMES_RESERVADOS = Set.of("me", "search");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int NOME_MAX = 60; // igual ao AccountService
    private static final int EMAIL_MAX = 254;
    private static final int SENHA_MIN = 8;
    private static final int SENHA_MAX_BYTES = 72; // o bcrypt ignora o que passar disso

    // força bruta numa conta: senhas erradas seguidas bloqueiam o login dela por um tempo,
    // não importa de quantos IPs venham (o limite por IP fica no RateLimitFilter)
    private static final int FALHAS_MAX = 10;
    private static final Duration JANELA_FALHAS = Duration.ofMinutes(15);

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private RateLimiter rateLimiter;

    public AuthResponseDTO registrar(RegisterRequestDTO request) {
        String nome = request.getName() == null ? "" : request.getName().trim();
        String username = request.getUsername() == null ? "" : request.getUsername().toLowerCase(Locale.ROOT).trim();
        String email = request.getEmail() == null ? "" : request.getEmail().trim().toLowerCase(Locale.ROOT);
        String senha = request.getPassword() == null ? "" : request.getPassword();

        if (nome.isEmpty()) throw new IllegalArgumentException("Informe seu nome.");
        if (nome.length() > NOME_MAX) throw new IllegalArgumentException("O nome pode ter até " + NOME_MAX + " caracteres.");

        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new IllegalArgumentException(
                    "Nome de usuário deve ter entre 3 e 20 caracteres, apenas letras minúsculas, números e _");
        }

        if (email.length() > EMAIL_MAX || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Informe um e-mail válido.");
        }

        if (senha.length() < SENHA_MIN) {
            throw new IllegalArgumentException("A senha precisa ter pelo menos " + SENHA_MIN + " caracteres.");
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > SENHA_MAX_BYTES) {
            throw new IllegalArgumentException("A senha é longa demais.");
        }

        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailJaCadastradoException(email);
        }

        if (USERNAMES_RESERVADOS.contains(username) || userRepository.existsByUsername(username)) {
            throw new UsernameJaCadastradoException(username);
        }

        String senhaCriptografada = passwordEncoder.encode(senha);
        User user = new User(nome, username, email, senhaCriptografada);
        user = userRepository.save(user);

        String token = jwtService.gerarToken(new CustomUserDetails(user));
        return new AuthResponseDTO(token, user.getName(), user.getUsername(), user.getEmail());
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        String login = request.getLogin() == null ? "" : request.getLogin().trim();
        String chaveFalhas = "login-falhas:" + login.toLowerCase(Locale.ROOT);
        if (rateLimiter.esgotado(chaveFalhas, FALHAS_MAX)) {
            throw new TooManyRequestsException("Muitas tentativas de login nesta conta. Espere alguns minutos e tente de novo.");
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(login, request.getPassword()));
        } catch (BadCredentialsException e) {
            rateLimiter.registrar(chaveFalhas, JANELA_FALHAS);
            throw e;
        }
        rateLimiter.zerar(chaveFalhas);

        User user = userRepository.findFirstByEmailIgnoreCase(login)
                .or(() -> userRepository.findByUsername(login.toLowerCase(Locale.ROOT)))
                .orElseThrow();

        String token = jwtService.gerarToken(new CustomUserDetails(user));
        return new AuthResponseDTO(token, user.getName(), user.getUsername(), user.getEmail());
    }

    public boolean usernameDisponivel(String username) {
        String limpo = username.toLowerCase().trim();
        return !USERNAMES_RESERVADOS.contains(limpo) && !userRepository.existsByUsername(limpo);
    }
}