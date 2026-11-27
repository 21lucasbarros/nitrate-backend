package com.lucasbarros.nitrate.services;

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
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// regras do cadastro, o limite de senhas erradas no login e o logout (sem banco)
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RevokedTokenRepository revokedTokenRepository;
    @Spy private RateLimiter rateLimiter = new RateLimiter();
    @InjectMocks private AuthService authService;

    private RegisterRequestDTO cadastro(String nome, String username, String email, String senha) {
        RegisterRequestDTO r = new RegisterRequestDTO();
        r.setName(nome);
        r.setUsername(username);
        r.setEmail(email);
        r.setPassword(senha);
        r.setAcceptedTerms(true);
        return r;
    }

    private String erro(RegisterRequestDTO r) {
        return assertThrows(IllegalArgumentException.class, () -> authService.registrar(r)).getMessage();
    }

    @Test
    void recusaSenhaCurtaOuVazia() {
        assertEquals("A senha precisa ter pelo menos 8 caracteres.", erro(cadastro("Ana", "ana", "ana@x.com", "")));
        assertEquals("A senha precisa ter pelo menos 8 caracteres.", erro(cadastro("Ana", "ana", "ana@x.com", "1234567")));
        assertEquals("A senha precisa ter pelo menos 8 caracteres.", erro(cadastro("Ana", "ana", "ana@x.com", null)));
    }

    @Test
    void recusaSenhaAlemDoQueOBcryptLe() {
        assertEquals("A senha é longa demais.", erro(cadastro("Ana", "ana", "ana@x.com", "a".repeat(73))));
    }

    @Test
    void recusaEmailInvalidoENomeVazioOuLongo() {
        assertEquals("Informe um e-mail válido.", erro(cadastro("Ana", "ana", "ana.x.com", "senha-boa-123")));
        assertEquals("Informe seu nome.", erro(cadastro("  ", "ana", "ana@x.com", "senha-boa-123")));
        assertEquals("O nome pode ter até 60 caracteres.", erro(cadastro("a".repeat(61), "ana", "ana@x.com", "senha-boa-123")));
    }

    @Test
    void emailEhGuardadoEmMinusculasEComparadoSemDiferenciar() {
        when(userRepository.existsByEmailIgnoreCase("ana@x.com")).thenReturn(false);
        when(userRepository.existsByUsernameIgnoreCase("Ana")).thenReturn(false);
        when(passwordEncoder.encode("senha-boa-123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        authService.registrar(cadastro(" Ana ", "Ana", "  Ana@X.com ", "senha-boa-123"));

        ArgumentCaptor<User> salvo = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(salvo.capture());
        assertEquals("ana@x.com", salvo.getValue().getEmail());
        assertEquals("Ana", salvo.getValue().getUsername()); // o @ guarda as maiúsculas
        assertEquals("Ana", salvo.getValue().getName());
        assertNotNull(salvo.getValue().getTermsAcceptedAt());
    }

    @Test
    void recusaCadastroSemAceitarOsTermos() {
        RegisterRequestDTO r = cadastro("Ana", "ana", "ana@x.com", "senha-boa-123");
        r.setAcceptedTerms(null);
        assertEquals("Confirme que tem pelo menos 13 anos e aceite os Termos de Uso e a Política de Privacidade.", erro(r));
        r.setAcceptedTerms(false);
        assertEquals("Confirme que tem pelo menos 13 anos e aceite os Termos de Uso e a Política de Privacidade.", erro(r));
        verify(userRepository, never()).save(any());
    }

    @Test
    void emailJaUsadoComOutraCaixaContaComoRepetido() {
        when(userRepository.existsByEmailIgnoreCase("ana@x.com")).thenReturn(true);
        assertThrows(EmailJaCadastradoException.class,
                () -> authService.registrar(cadastro("Ana", "ana", "ANA@x.com", "senha-boa-123")));
    }

    private LoginRequestDTO login(String login) {
        LoginRequestDTO r = new LoginRequestDTO();
        r.setLogin(login);
        r.setPassword("errada");
        return r;
    }

    private User ana() {
        User ana = new User("Ana", "ana", "ana@x.com", "hash");
        ana.setId(7L);
        return ana;
    }

    @Test
    void dezSenhasErradasDoMesmoIpBloqueiamOLoginDaContaPorEsseIp() {
        lenient().when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("x"));
        lenient().when(userRepository.findFirstByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        lenient().when(userRepository.findFirstByEmailIgnoreCase("Ana@x.com")).thenReturn(Optional.of(ana()));
        lenient().when(userRepository.findFirstByUsernameIgnoreCase("ana")).thenReturn(Optional.of(ana()));

        for (int i = 0; i < 10; i++) {
            assertThrows(BadCredentialsException.class, () -> authService.login(login("Ana@x.com"), "1.1.1.1"));
        }
        // a 11ª nem chega a conferir a senha, nem entrando pelo @ (é a mesma conta)
        assertThrows(TooManyRequestsException.class, () -> authService.login(login("ana"), "1.1.1.1"));
        verify(authenticationManager, times(10)).authenticate(any());

        // de outro IP a dona da conta ainda consegue tentar: errar de propósito não tranca a conta dos outros
        assertThrows(BadCredentialsException.class, () -> authService.login(login("ana"), "2.2.2.2"));
    }

    @Test
    void muitasSenhasErradasDeIpsDiferentesBloqueiamAConta() {
        lenient().when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("x"));
        when(userRepository.findFirstByEmailIgnoreCase("ana@x.com")).thenReturn(Optional.of(ana()));

        for (int i = 0; i < 50; i++) {
            String ip = "10.0.0." + i;
            assertThrows(BadCredentialsException.class, () -> authService.login(login("ana@x.com"), ip));
        }
        assertThrows(TooManyRequestsException.class, () -> authService.login(login("ana@x.com"), "10.0.1.1"));
    }

    @Test
    void logoutEncerraOToken() {
        Instant expira = Instant.now().plusSeconds(3600);
        when(jwtService.ler("abc")).thenReturn(new JwtService.Sessao(7L, "jti-1", Instant.now(), expira, false));

        authService.sair("Bearer abc");

        ArgumentCaptor<RevokedToken> salvo = ArgumentCaptor.forClass(RevokedToken.class);
        verify(revokedTokenRepository).save(salvo.capture());
        assertEquals("jti-1", salvo.getValue().getId());
        assertEquals(expira, salvo.getValue().getExpiresAt());
    }

    @Test
    void logoutSemTokenOuComTokenInvalidoNaoFazNada() {
        when(jwtService.ler("lixo")).thenThrow(new MalformedJwtException("x"));
        authService.sair(null);
        authService.sair("Bearer lixo");
        verify(revokedTokenRepository, never()).save(any());
    }
}
