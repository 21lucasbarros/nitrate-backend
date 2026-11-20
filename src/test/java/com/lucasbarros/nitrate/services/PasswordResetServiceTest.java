package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.entities.PasswordResetToken;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.PasswordResetTokenRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.security.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// pedido e uso do link de redefinir a senha (sem banco e sem enviar e-mail)
@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Spy private RateLimiter rateLimiter = new RateLimiter();
    @InjectMocks private PasswordResetService service;

    private final User ana = new User("Ana", "ana", "ana@x.com", "hash-antigo");

    @BeforeEach
    void configura() {
        ReflectionTestUtils.setField(service, "frontendUrl", "https://nitrate.app/");
    }

    @Test
    void emailSemContaNaoEnviaNadaNemDaErro() {
        when(userRepository.findFirstByEmailIgnoreCase("ninguem@x.com")).thenReturn(Optional.empty());
        service.solicitar(" Ninguem@X.com ");
        verify(tokenRepository, never()).save(any());
        verify(emailService, never()).enviarEmSegundoPlano(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void enviaLinkComOTokenEGuardaSoOHash() {
        when(userRepository.findFirstByEmailIgnoreCase("ana@x.com")).thenReturn(Optional.of(ana));

        service.solicitar("Ana@x.com");

        ArgumentCaptor<PasswordResetToken> salvo = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(tokenRepository).save(salvo.capture());
        verify(tokenRepository).deleteAllByUser(ana); // o link anterior deixa de valer
        ArgumentCaptor<String> texto = ArgumentCaptor.forClass(String.class);
        verify(emailService).enviarEmSegundoPlano(eq("ana@x.com"), anyString(), anyString(), texto.capture());

        String token = texto.getValue().replaceAll("(?s).*redefinir-senha\\?token=(\\S+).*", "$1");
        assertTrue(texto.getValue().contains("https://nitrate.app/redefinir-senha?token="));
        assertEquals(PasswordResetService.hash(token), salvo.getValue().getTokenHash());
        assertTrue(salvo.getValue().getExpiresAt().isAfter(Instant.now().plus(Duration.ofMinutes(29))));
    }

    @Test
    void maisDeTresPedidosPorHoraParaOMesmoEmailSaoIgnorados() {
        when(userRepository.findFirstByEmailIgnoreCase("ana@x.com")).thenReturn(Optional.of(ana));
        for (int i = 0; i < 5; i++) service.solicitar("ana@x.com");
        verify(emailService, times(3)).enviarEmSegundoPlano(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    void tokenValidoTrocaASenhaEDerrubaAsSessoes() {
        PasswordResetToken pedido = new PasswordResetToken(ana, PasswordResetService.hash("abc"), Instant.now().plusSeconds(600));
        when(tokenRepository.findByTokenHash(PasswordResetService.hash("abc"))).thenReturn(Optional.of(pedido));
        when(passwordEncoder.encode("senha-nova-123")).thenReturn("hash-novo");

        service.redefinir("abc", "senha-nova-123");

        assertEquals("hash-novo", ana.getPasswordHash());
        assertNotNull(ana.getPasswordChangedAt());
        verify(userRepository).save(ana);
        verify(tokenRepository).deleteAllByUser(ana);
    }

    @Test
    void tokenExpiradoOuDesconhecidoEhRecusado() {
        PasswordResetToken velho = new PasswordResetToken(ana, PasswordResetService.hash("velho"), Instant.now().minusSeconds(1));
        when(tokenRepository.findByTokenHash(PasswordResetService.hash("velho"))).thenReturn(Optional.of(velho));
        when(tokenRepository.findByTokenHash(PasswordResetService.hash("outro"))).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.redefinir("velho", "senha-nova-123"));
        assertThrows(IllegalArgumentException.class, () -> service.redefinir("outro", "senha-nova-123"));
        assertThrows(IllegalArgumentException.class, () -> service.redefinir(null, "senha-nova-123"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void senhaNovaSegueAsRegrasDoCadastro() {
        PasswordResetToken pedido = new PasswordResetToken(ana, PasswordResetService.hash("abc"), Instant.now().plusSeconds(600));
        when(tokenRepository.findByTokenHash(PasswordResetService.hash("abc"))).thenReturn(Optional.of(pedido));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> service.redefinir("abc", "curta"));
        assertEquals("A senha precisa ter pelo menos 8 caracteres.", e.getMessage());
        verify(userRepository, never()).save(any());
    }
}
