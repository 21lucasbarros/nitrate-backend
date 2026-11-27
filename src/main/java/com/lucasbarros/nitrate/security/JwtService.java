package com.lucasbarros.nitrate.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

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

    private SecretKey chave() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String gerarToken(UserDetails userDetails) {
        return gerarToken(userDetails, false);
    }

    /** com `lembrar`, o token dura rememberExpirationMs e leva a marca para ser renovado igual */
    public String gerarToken(UserDetails userDetails, boolean lembrar) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + (lembrar ? rememberExpirationMs : expirationMs));

        var builder = Jwts.builder()
                .subject(userDetails.getUsername()) // aqui "username" é o email
                .issuedAt(agora)
                .expiration(expiracao);
        if (lembrar) builder.claim(CLAIM_LEMBRAR, true);
        return builder.signWith(chave()).compact();
    }

    /**
     * Se o token foi emitido com "Lembrar de mim". Usado ao trocar senha ou e-mail, que devolvem
     * um token novo com a mesma duração do atual. Token inválido conta como não.
     */
    public boolean lembrar(String token) {
        try {
            return Boolean.TRUE.equals(Jwts.parser()
                    .verifyWith(chave())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .get(CLAIM_LEMBRAR, Boolean.class));
        } catch (RuntimeException e) {
            return false;
        }
    }

    public String extrairEmail(String token) {
        return Jwts.parser()
                .verifyWith(chave())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean tokenValido(String token, UserDetails userDetails) {
        String email = extrairEmail(token);
        return email.equals(userDetails.getUsername()) && !tokenExpirado(token) && !emitidoAntesDaTrocaDeSenha(token, userDetails);
    }

    /** trocar a senha (ex.: pela redefinição) derruba as sessões abertas em outros aparelhos */
    private boolean emitidoAntesDaTrocaDeSenha(String token, UserDetails userDetails) {
        if (!(userDetails instanceof CustomUserDetails detalhes)) return false;
        Instant trocou = detalhes.getUser().getPasswordChangedAt();
        if (trocou == null) return false;
        Date emitido = Jwts.parser()
                .verifyWith(chave())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getIssuedAt();
        // o iat do JWT tem precisão de segundos: compara no mesmo nível
        return emitido == null || emitido.toInstant().isBefore(trocou.truncatedTo(ChronoUnit.SECONDS));
    }

    private boolean tokenExpirado(String token) {
        Date expiracao = Jwts.parser()
                .verifyWith(chave())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        return expiracao.before(new Date());
    }
}
