package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.SeriesReviewDTO;
import com.lucasbarros.nitrate.entities.Notification;
import com.lucasbarros.nitrate.entities.ReviewCollaboration.Status;
import com.lucasbarros.nitrate.entities.Series;
import com.lucasbarros.nitrate.entities.SeriesReview;
import com.lucasbarros.nitrate.entities.SeriesReviewCollaboration;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.FollowRepository;
import com.lucasbarros.nitrate.repositories.NotificationRepository;
import com.lucasbarros.nitrate.repositories.SeriesReviewCollaborationRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.services.exceptions.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

// registro de série em conjunto: as mesmas regras das reviews de filme (sem banco)
@ExtendWith(MockitoExtension.class)
class SeriesReviewCollaborationServiceTest {

    @Mock private SeriesReviewCollaborationRepository collaborationRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private FollowRepository followRepository;
    // quem confere os convidados é o serviço dos filmes, com os mesmos mocks
    @InjectMocks private ReviewCollaborationService reviewCollaborationService;
    @InjectMocks private SeriesReviewCollaborationService service;

    private final AtomicLong ids = new AtomicLong(100);
    private User ana;
    private User bia;
    private User caio;
    private SeriesReview daAna;

    @BeforeEach
    void configura() {
        ReflectionTestUtils.setField(service, "reviewCollaborationService", reviewCollaborationService);
        ana = usuario(1L, "ana");
        bia = usuario(2L, "bia");
        caio = usuario(3L, "caio");
        daAna = registro(10L, ana, "Que temporada!");
        lenient().when(collaborationRepository.save(any(SeriesReviewCollaboration.class))).thenAnswer(inv -> {
            SeriesReviewCollaboration c = inv.getArgument(0);
            if (c.getId() == null) c.setId(ids.incrementAndGet());
            return c;
        });
    }

    private User usuario(Long id, String username) {
        User u = new User(username, username, username + "@x.com", "hash");
        u.setId(id);
        lenient().when(userRepository.findFirstByUsernameIgnoreCase(username)).thenReturn(Optional.of(u));
        return u;
    }

    private static SeriesReview registro(Long id, User user, String texto) {
        SeriesReview r = new SeriesReview(user, new Series());
        r.setId(id);
        r.setSeasonNumber(2);
        r.setText(texto);
        return r;
    }

    private void seguem(User... quem) {
        for (User u : quem) lenient().when(followRepository.existsByFollowerAndFollowed(u, ana)).thenReturn(true);
    }

    /** o convite de `quem` aceito, com o registro dela (copiando o texto da Ana, se `compartilhado`) */
    private SeriesReviewCollaboration aceita(User quem, boolean compartilhado) {
        seguem(quem);
        List<String> nomes = new java.util.ArrayList<>(daAna.getInvites().stream().map(c -> c.getInvitee().getUsername()).toList());
        nomes.add(quem.getUsername());
        service.definirConvites(ana, daAna, nomes);
        SeriesReviewCollaboration c = daAna.getInvites().get(daAna.getInvites().size() - 1);
        SeriesReview dela = registro(ids.incrementAndGet(), quem, compartilhado ? daAna.getText() : "Outra opinião");
        service.concluirAceite(c, dela, compartilhado);
        return c;
    }

    private List<String> convidados() {
        return daAna.getInvites().stream().map(c -> c.getInvitee().getUsername()).toList();
    }

    @Test
    void marcaVariasPessoasEAvisaCadaUma() {
        seguem(bia, caio);
        service.definirConvites(ana, daAna, List.of(" @bia ", "CAIO", "bia"));
        assertEquals(List.of("bia", "caio"), convidados());
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void naoMarcaQuemNaoSegue() {
        seguem(bia);
        assertThrows(IllegalArgumentException.class, () -> service.definirConvites(ana, daAna, List.of("bia", "caio")));
        assertTrue(daAna.getInvites().isEmpty());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void quemJaAceitouNaoSaiDaLista() {
        aceita(bia, true);
        assertThrows(ConflictException.class, () -> service.definirConvites(ana, daAna, List.of()));
        assertEquals(Status.ACCEPTED, daAna.getInvites().get(0).getStatus());
    }

    @Test
    void edicaoDoTextoOriginalChegaACopia() {
        SeriesReviewCollaboration daBia = aceita(bia, true);
        daAna.setText("Mudei de ideia.");
        service.aoAtualizar(daAna);
        assertEquals("Mudei de ideia.", daBia.getInviteeReview().getText());
    }

    @Test
    void quemConvidouApagaEASessaoSeDesfaz() {
        SeriesReviewCollaboration daBia = aceita(bia, false);
        service.aoApagar(daAna);
        assertTrue(daAna.getInvites().isEmpty());
        assertNull(daBia.getInviteeReview().getCollaboration());
        verify(notificationRepository).deleteBySeriesCollaboration(daBia);
    }

    @Test
    void cadaPessoaVeAsOutrasDaSessao() {
        SeriesReviewCollaboration daBia = aceita(bia, false);
        seguem(caio);
        service.definirConvites(ana, daAna, List.of("bia", "caio"));

        var daAnaDTO = new SeriesReviewDTO(daAna).together();
        assertTrue(daAnaDTO.invitedByMe());
        assertEquals(2, daAnaDTO.partners().size());

        var daBiaDTO = new SeriesReviewDTO(daBia.getInviteeReview()).together();
        assertFalse(daBiaDTO.invitedByMe());
        assertEquals("ana", daBiaDTO.invitedBy().getUsername());
        assertEquals(1, daBiaDTO.partners().size()); // o Caio ainda não aceitou
    }
}
