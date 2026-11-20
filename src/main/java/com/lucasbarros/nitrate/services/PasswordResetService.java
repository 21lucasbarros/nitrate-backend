package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.entities.PasswordResetToken;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.PasswordResetTokenRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.security.RateLimiter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

/**
 * "Esqueci minha senha": manda por e-mail um link com um token de uso único, que vale por
 * pouco tempo, e troca a senha de quem o apresentar.
 */
@Service
public class PasswordResetService {

    private static final Duration VALIDADE = Duration.ofMinutes(30);
    // por e-mail, além do limite por IP do RateLimitFilter: ninguém lota a caixa de outra pessoa
    private static final int PEDIDOS_MAX_POR_EMAIL = 3;
    private static final Duration JANELA_PEDIDOS = Duration.ofHours(1);
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired private UserRepository userRepository;
    @Autowired private PasswordResetTokenRepository tokenRepository;
    @Autowired private EmailService emailService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private RateLimiter rateLimiter;

    // endereço do front, onde fica a página /redefinir-senha
    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    /**
     * Sempre termina sem erro, exista ou não a conta: a resposta igual para todo mundo impede
     * descobrir quais e-mails estão cadastrados.
     */
    @Transactional
    public void solicitar(String emailInformado) {
        String email = emailInformado == null ? "" : emailInformado.trim().toLowerCase(Locale.ROOT);
        if (email.isEmpty() || !rateLimiter.tentar("reset-email:" + email, PEDIDOS_MAX_POR_EMAIL, JANELA_PEDIDOS)) {
            return;
        }
        Optional<User> encontrado = userRepository.findFirstByEmailIgnoreCase(email);
        if (encontrado.isEmpty()) return;
        User user = encontrado.get();

        Instant agora = Instant.now();
        tokenRepository.deleteAllExpiredBefore(agora);
        tokenRepository.deleteAllByUser(user); // só o link mais recente vale
        String token = gerarToken();
        tokenRepository.save(new PasswordResetToken(user, hash(token), agora.plus(VALIDADE)));

        String link = frontendUrl.replaceAll("/+$", "") + "/redefinir-senha?token=" + token;
        emailService.enviarEmSegundoPlano(user.getEmail(), "Redefinir sua senha do Nitrate",
                html(user.getName(), link), texto(user.getName(), link));
    }

    @Transactional
    public void redefinir(String token, String novaSenha) {
        PasswordResetToken pedido = token == null || token.isBlank()
                ? null
                : tokenRepository.findByTokenHash(hash(token.trim())).orElse(null);
        Instant agora = Instant.now();
        if (pedido == null || pedido.expirado(agora)) {
            throw new IllegalArgumentException("Este link é inválido ou expirou. Peça um novo.");
        }
        AuthService.validarSenha(novaSenha);

        User user = pedido.getUser();
        user.setPasswordHash(passwordEncoder.encode(novaSenha));
        user.setPasswordChangedAt(agora); // derruba as sessões abertas
        userRepository.save(user);
        tokenRepository.deleteAllByUser(user);
        // quem estava bloqueado por senhas erradas volta a conseguir entrar com a nova
        rateLimiter.zerar("login-falhas:" + user.getEmail().toLowerCase(Locale.ROOT));
        rateLimiter.zerar("login-falhas:" + user.getUsername().toLowerCase(Locale.ROOT));
    }

    private static String gerarToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // todo JDK tem SHA-256
        }
    }

    private static String texto(String nome, String link) {
        return "Olá, " + nome + "!\n\n"
                + "Recebemos um pedido para redefinir a senha da sua conta no Nitrate. "
                + "Para escolher uma nova senha, abra o link abaixo (ele vale por 30 minutos):\n\n"
                + link + "\n\n"
                + "Se não foi você, ignore este e-mail: sua senha continua a mesma.";
    }

    private static String html(String nome, String link) {
        String nomeSeguro = HtmlUtils.htmlEscape(nome);
        String linkSeguro = HtmlUtils.htmlEscape(link);
        return """
                <div style="font-family: -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px 24px; color: #1a1b1e;">
                  <p style="font-size: 20px; font-weight: 700; letter-spacing: -0.5px; text-transform: uppercase; margin: 0 0 24px;">Nitrate<span style="color: #FF7A33;">.</span></p>
                  <p style="font-size: 16px; line-height: 1.6;">Olá, %s!</p>
                  <p style="font-size: 16px; line-height: 1.6;">Recebemos um pedido para redefinir a senha da sua conta. Clique no botão para escolher uma nova senha. O link vale por 30 minutos.</p>
                  <p style="margin: 32px 0;"><a href="%s" style="background: #FF6A1C; color: #141517; text-decoration: none; font-weight: 600; padding: 14px 28px; border-radius: 999px; display: inline-block;">Redefinir senha</a></p>
                  <p style="font-size: 14px; line-height: 1.6; color: #6b6f76;">Se o botão não funcionar, copie e cole este endereço no navegador:<br><a href="%s" style="color: #3b6fd8; word-break: break-all;">%s</a></p>
                  <p style="font-size: 14px; line-height: 1.6; color: #6b6f76;">Se não foi você, ignore este e-mail: sua senha continua a mesma.</p>
                </div>
                """.formatted(nomeSeguro, linkSeguro, linkSeguro, linkSeguro);
    }
}
