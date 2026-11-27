package com.lucasbarros.nitrate.security;

import com.lucasbarros.nitrate.entities.User;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

// duração do token com e sem "Lembrar de mim", a conta pelo id e as sessões encerradas
class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres";
    private static final long DIA = 86_400_000L;
    private static final long TRINTA_DIAS = 30 * DIA;

    private JwtService jwtService;
    private User ana;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SEGREDO);
        ReflectionTestUtils.setField(jwtService, "expirationMs", DIA);
        ReflectionTestUtils.setField(jwtService, "rememberExpirationMs", TRINTA_DIAS);
        ana = new User("Ana", "ana", "ana@email.com", "x");
        ana.setId(7L);
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
        assertEquals(7L, jwtService.ler(token).userId());
    }

    @Test
    void tokenInvalidoNaoContaComoLembrar() {
        assertFalse(jwtService.lembrar("nao-e-um-jwt"));
    }

    @Test
    void cadaTokenTemUmIdProprio() {
        assertNotEquals(jwtService.ler(jwtService.gerarToken(ana)).id(), jwtService.ler(jwtService.gerarToken(ana)).id());
    }

    @Test
    void tokenAntigoComOEmailNoLugarDoIdEhRecusado() {
        String antigo = Jwts.builder()
                .subject("ana@email.com")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + DIA))
                .signWith(Keys.hmacShaKeyFor(SEGREDO.getBytes()))
                .compact();
        assertThrows(JwtException.class, () -> jwtService.ler(antigo));
    }

    @Test
    void naoValeParaOutraConta() {
        JwtService.Sessao sessao = jwtService.ler(jwtService.gerarToken(ana));
        User outra = new User("Bia", "bia", "ana@email.com", "x"); // mesmo e-mail, outra conta
        outra.setId(8L);
        assertTrue(jwtService.valeParaConta(sessao, ana));
        assertFalse(jwtService.valeParaConta(sessao, outra));
    }

    @Test
    void sessoesEncerradasDerrubamTokensEmitidosAntes() {
        JwtService.Sessao sessao = jwtService.ler(jwtService.gerarToken(ana));
        ana.setSessionsRevokedAt(Instant.now().plus(Duration.ofSeconds(2)));
        assertFalse(jwtService.valeParaConta(sessao, ana));

        ana.setSessionsRevokedAt(Instant.now().minus(Duration.ofSeconds(2)));
        assertTrue(jwtService.valeParaConta(sessao, ana));
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
