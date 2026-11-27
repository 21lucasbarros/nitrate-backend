package com.lucasbarros.nitrate.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// duração do token com e sem "Lembrar de mim"
class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres";
    private static final long DIA = 86_400_000L;
    private static final long TRINTA_DIAS = 30 * DIA;

    private JwtService jwtService;
    private final UserDetails ana = new User("ana@email.com", "x", List.of());

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SEGREDO);
        ReflectionTestUtils.setField(jwtService, "expirationMs", DIA);
        ReflectionTestUtils.setField(jwtService, "rememberExpirationMs", TRINTA_DIAS);
    }

    @Test
    void semLembrarDura24h() {
        String token = jwtService.gerarToken(ana);
        assertFalse(jwtService.lembrar(token));
        assertEquals(DIA, duracao(token), 2_000);
    }

    @Test
    void comLembrarDura30Dias() {
        String token = jwtService.gerarToken(ana, true);
        assertTrue(jwtService.lembrar(token));
        assertEquals(TRINTA_DIAS, duracao(token), 2_000);
        assertEquals("ana@email.com", jwtService.extrairEmail(token));
    }

    @Test
    void tokenInvalidoNaoContaComoLembrar() {
        assertFalse(jwtService.lembrar("nao-e-um-jwt"));
    }

    private long duracao(String token) {
        Date exp = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SEGREDO.getBytes()))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration();
        return exp.getTime() - System.currentTimeMillis();
    }
}
