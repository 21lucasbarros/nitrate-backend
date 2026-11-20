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

    private SecretKey chave() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String gerarToken(UserDetails userDetails) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expirationMs);

        return Jwts.builder()
                .subject(userDetails.getUsername()) // aqui "username" é o email
                .issuedAt(agora)
                .expiration(expiracao)
                .signWith(chave())
                .compact();
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
