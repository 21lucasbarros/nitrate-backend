package com.lucasbarros.nitrate.security;

import com.lucasbarros.nitrate.entities.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

/**
 * O token de login. Identifica a conta pelo id, que nunca muda nem é reaproveitado: com o e-mail,
 * um token antigo de um e-mail que foi trocado ou de uma conta excluída abriria a conta de quem se
 * cadastrasse depois com aquele e-mail. Cada token tem um id próprio (jti), para o logout derrubar
 * só ele (RevokedToken).
 */
@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    // "Lembrar de mim" no login: token longo, de 30 dias por padrão
    @Value("${jwt.remember-expiration-ms:2592000000}")
    private long rememberExpirationMs;

    private static final String CLAIM_LEMBRAR = "lembrar";

    /** o que o token diz, depois de conferida a assinatura e a validade */
    public record Sessao(Long userId, String id, Instant emitidoEm, Instant expiraEm, boolean lembrar) {}

    private SecretKey chave() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String gerarToken(User user) {
        return gerarToken(user, false);
    }

    /** com `lembrar`, o token dura rememberExpirationMs e leva a marca para ser renovado igual */
    public String gerarToken(User user, boolean lembrar) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + (lembrar ? rememberExpirationMs : expirationMs));

        var builder = Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .id(UUID.randomUUID().toString())
                .issuedAt(agora)
                .expiration(expiracao);
        if (lembrar) builder.claim(CLAIM_LEMBRAR, true);
        return builder.signWith(chave()).compact();
    }

    /**
     * Confere a assinatura e a validade (o parser recusa token vencido) e devolve o conteúdo.
     * Tokens do formato antigo, com o e-mail no lugar do id, são recusados: quem tinha um
     * entra de novo.
     */
    public Sessao ler(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(chave())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Long userId;
        try {
            userId = Long.valueOf(claims.getSubject());
        } catch (NumberFormatException e) {
            throw new MalformedJwtException("token sem o id da conta");
        }
        if (claims.getId() == null || claims.getIssuedAt() == null || claims.getExpiration() == null) {
            throw new MalformedJwtException("token incompleto");
        }
        return new Sessao(userId, claims.getId(), claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant(), Boolean.TRUE.equals(claims.get(CLAIM_LEMBRAR, Boolean.class)));
    }

    /**
     * Se o token foi emitido com "Lembrar de mim". Usado ao trocar senha ou e-mail, que devolvem
     * um token novo com a mesma duração do atual. Token inválido conta como não.
     */
    public boolean lembrar(String token) {
        try {
            return ler(token).lembrar();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /**
     * O token é desta conta e não foi emitido antes de as sessões dela serem encerradas
     * (troca de senha, redefinição ou troca de e-mail).
     */
    public boolean valeParaConta(Sessao sessao, User user) {
        if (!sessao.userId().equals(user.getId())) return false;
        Instant encerradas = user.getSessionsRevokedAt();
        // o iat do JWT tem precisão de segundos: compara no mesmo nível
        return encerradas == null || !sessao.emitidoEm().isBefore(encerradas.truncatedTo(ChronoUnit.SECONDS));
    }
}
