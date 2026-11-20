package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.AuthResponseDTO;
import com.lucasbarros.nitrate.dto.PasswordChangeDTO;
import com.lucasbarros.nitrate.dto.ProfileUpdateDTO;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.PasswordResetTokenRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.security.JwtService;
import com.lucasbarros.nitrate.security.RateLimiter;
import com.lucasbarros.nitrate.services.exceptions.ForbiddenException;
import com.lucasbarros.nitrate.services.exceptions.TooManyRequestsException;
import com.lucasbarros.nitrate.services.exceptions.UsernameJaCadastradoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// trocar o @ no perfil e trocar a senha sabendo a atual (sem banco e sem enviar e-mail)
@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private EmailService emailService;
    @Mock private UserService userService;
    @Spy private RateLimiter rateLimiter = new RateLimiter();
    @InjectMocks private AccountService accountService;

    private User ana;

    @BeforeEach
    void configura() {
        ana = new User("Ana", "ana", "ana@x.com", "hash-atual");
        ana.setId(1L);
        lenient().when(userRepository.findById(1L)).thenReturn(Optional.of(ana));
        lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private ProfileUpdateDTO perfil(String username) {
        return new ProfileUpdateDTO("Ana", username, null, null, null);
    }

    @Test
    void trocaOArrobaGuardandoAsMaiusculas() {
        when(userRepository.existsByUsernameIgnoreCase("Ana_Nova")).thenReturn(false);
        accountService.atualizarPerfil(ana, perfil(" Ana_Nova "));
        assertEquals("Ana_Nova", ana.getUsername());
    }

    @Test
    void mudarSoAsMaiusculasDoProprioArrobaNaoConflitaComEleMesmo() {
        accountService.atualizarPerfil(ana, perfil("ANA"));
        assertEquals("ANA", ana.getUsername());
        verify(userRepository, never()).existsByUsernameIgnoreCase(anyString());
    }

    @Test
    void arrobaNuloOuIgualNaoMudaNemConsulta() {
        accountService.atualizarPerfil(ana, perfil(null));
        accountService.atualizarPerfil(ana, perfil("ana"));
        assertEquals("ana", ana.getUsername());
        verify(userRepository, never()).existsByUsernameIgnoreCase(anyString());
    }

    @Test
    void arrobaEmUsoReservadoOuInvalidoEhRecusado() {
        when(userRepository.existsByUsernameIgnoreCase("Bia")).thenReturn(true);
        assertThrows(UsernameJaCadastradoException.class, () -> accountService.atualizarPerfil(ana, perfil("Bia")));
        assertThrows(UsernameJaCadastradoException.class, () -> accountService.atualizarPerfil(ana, perfil("Search")));
        assertThrows(IllegalArgumentException.class, () -> accountService.atualizarPerfil(ana, perfil("a")));
        assertThrows(IllegalArgumentException.class, () -> accountService.atualizarPerfil(ana, perfil("ana.silva")));
        assertEquals("ana", ana.getUsername());
    }

    @Test
    void trocaASenhaDerrubaAsSessoesEDevolveTokenNovo() {
        when(passwordEncoder.matches("senha-atual", "hash-atual")).thenReturn(true);
        when(passwordEncoder.matches("senha-nova-123", "hash-atual")).thenReturn(false);
        when(passwordEncoder.encode("senha-nova-123")).thenReturn("hash-novo");
        when(jwtService.gerarToken(any())).thenReturn("token-novo");

        AuthResponseDTO resposta = accountService.trocarSenha(ana, new PasswordChangeDTO("senha-atual", "senha-nova-123"));

        assertEquals("token-novo", resposta.getToken());
        assertEquals("hash-novo", ana.getPasswordHash());
        assertNotNull(ana.getPasswordChangedAt());
        verify(passwordResetTokenRepository).deleteAllByUser(ana);
        verify(emailService).enviarEmSegundoPlano(eq("ana@x.com"), anyString(), anyString(), anyString());
    }

    @Test
    void senhaAtualErradaNaoTrocaNada() {
        when(passwordEncoder.matches("errada", "hash-atual")).thenReturn(false);
        assertThrows(ForbiddenException.class,
                () -> accountService.trocarSenha(ana, new PasswordChangeDTO("errada", "senha-nova-123")));
        assertEquals("hash-atual", ana.getPasswordHash());
        verify(userRepository, never()).save(any());
    }

    @Test
    void novaSenhaCurtaOuIgualAAtualEhRecusada() {
        when(passwordEncoder.matches("senha-atual", "hash-atual")).thenReturn(true);
        IllegalArgumentException curta = assertThrows(IllegalArgumentException.class,
                () -> accountService.trocarSenha(ana, new PasswordChangeDTO("senha-atual", "curta")));
        assertEquals("A senha precisa ter pelo menos 8 caracteres.", curta.getMessage());
        IllegalArgumentException igual = assertThrows(IllegalArgumentException.class,
                () -> accountService.trocarSenha(ana, new PasswordChangeDTO("senha-atual", "senha-atual")));
        assertEquals("A nova senha precisa ser diferente da atual.", igual.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    void muitasTentativasSeguidasSaoBloqueadas() {
        when(passwordEncoder.matches("errada", "hash-atual")).thenReturn(false);
        for (int i = 0; i < 10; i++) {
            assertThrows(ForbiddenException.class,
                    () -> accountService.trocarSenha(ana, new PasswordChangeDTO("errada", "senha-nova-123")));
        }
        assertThrows(TooManyRequestsException.class,
                () -> accountService.trocarSenha(ana, new PasswordChangeDTO("errada", "senha-nova-123")));
    }
}
