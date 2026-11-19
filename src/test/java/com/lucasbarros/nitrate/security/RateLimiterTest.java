package com.lucasbarros.nitrate.security;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimiterTest {

    /** relógio que só anda quando o teste manda */
    private static class RelogioManual extends Clock {
        private Instant agora = Instant.parse("2026-01-01T00:00:00Z");
        void avancar(Duration d) { agora = agora.plus(d); }
        @Override public Instant instant() { return agora; }
        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
    }

    private final RelogioManual relogio = new RelogioManual();
    private final RateLimiter limiter = new RateLimiter(relogio);

    @Test
    void bloqueiaDepoisDoLimiteEVoltaQuandoAJanelaVence() {
        for (int i = 0; i < 3; i++) assertTrue(limiter.tentar("ip:1", 3, Duration.ofMinutes(1)));
        assertFalse(limiter.tentar("ip:1", 3, Duration.ofMinutes(1)));

        relogio.avancar(Duration.ofMinutes(1));
        assertTrue(limiter.tentar("ip:1", 3, Duration.ofMinutes(1)));
    }

    @Test
    void chavesSaoIndependentes() {
        assertTrue(limiter.tentar("ip:1", 1, Duration.ofMinutes(1)));
        assertFalse(limiter.tentar("ip:1", 1, Duration.ofMinutes(1)));
        assertTrue(limiter.tentar("ip:2", 1, Duration.ofMinutes(1)));
    }

    @Test
    void esgotadoSoConsultaSemContar() {
        assertFalse(limiter.esgotado("conta", 2));
        assertEquals(1, limiter.registrar("conta", Duration.ofMinutes(15)));
        assertEquals(2, limiter.registrar("conta", Duration.ofMinutes(15)));
        assertTrue(limiter.esgotado("conta", 2));
        assertTrue(limiter.esgotado("conta", 2)); // consultar de novo não muda nada

        limiter.zerar("conta");
        assertFalse(limiter.esgotado("conta", 2));
    }
}
