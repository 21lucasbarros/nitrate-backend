package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.ReviewDTO;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Notification;
import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.ReviewCollaboration;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.FollowRepository;
import com.lucasbarros.nitrate.repositories.NotificationRepository;
import com.lucasbarros.nitrate.repositories.ReviewCollaborationRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.services.exceptions.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
import static org.mockito.Mockito.when;

// review em conjunto: quem pode ser marcado, troca dos convites, a sessão com várias pessoas e o
// texto compartilhado (sem banco)
@ExtendWith(MockitoExtension.class)
class ReviewCollaborationServiceTest {

    @Mock private ReviewCollaborationRepository collaborationRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private FollowRepository followRepository;
    @InjectMocks private ReviewCollaborationService service;

    private final AtomicLong ids = new AtomicLong(100);
    private User ana;
    private User bia;
    private User caio;
    private Review daAna;

    @BeforeEach
    void configura() {
        ana = usuario(1L, "ana");
        bia = usuario(2L, "bia");
        caio = usuario(3L, "caio");
        daAna = review(10L, ana, "Que filme!");
        // o banco daria um id a cada convite salvo
        lenient().when(collaborationRepository.save(any(ReviewCollaboration.class))).thenAnswer(inv -> {
            ReviewCollaboration c = inv.getArgument(0);
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

    private static Review review(Long id, User user, String texto) {
        Review r = new Review(user, new Movie());
        r.setId(id);
        r.setText(texto);
        return r;
    }

    private void seguem(User... quem) {
        for (User u : quem) lenient().when(followRepository.existsByFollowerAndFollowed(u, ana)).thenReturn(true);
    }

    private List<String> convidados() {
        return daAna.getInvites().stream().map(c -> c.getInvitee().getUsername()).toList();
    }

    @Test
    void marcaVariasPessoasDeUmaVezEAvisaCadaUma() {
        seguem(bia, caio);
        service.definirConvites(ana, daAna, List.of(" @bia ", "CAIO", "bia"));
        assertEquals(List.of("bia", "caio"), convidados());
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void naoMarcaQuemNaoSegueNemMandaOsOutrosConvites() {
        seguem(bia);
        when(followRepository.existsByFollowerAndFollowed(caio, ana)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.definirConvites(ana, daAna, List.of("bia", "caio")));
        assertTrue(daAna.getInvites().isEmpty());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void marcaAteQuinzePessoas() {
        List<String> dezesseis = java.util.stream.IntStream.rangeClosed(1, 16).mapToObj(i -> "pessoa" + i).toList();
        assertThrows(IllegalArgumentException.class, () -> service.definirConvites(ana, daAna, dezesseis));
        assertTrue(daAna.getInvites().isEmpty());
        verify(userRepository, never()).findFirstByUsernameIgnoreCase("pessoa1");
    }

    @Test
    void naoMarcaASiMesmo() {
        assertThrows(IllegalArgumentException.class, () -> service.definirConvites(ana, daAna, List.of("ana")));
    }

    @Test
    void listaNovaMantemQuemContinuaEDesfazQuemSaiu() {
        seguem(bia, caio);
        service.definirConvites(ana, daAna, List.of("bia"));
        ReviewCollaboration daBia = daAna.getInvites().get(0);
        service.definirConvites(ana, daAna, List.of("bia", "caio"));
        assertEquals(List.of("bia", "caio"), convidados());
        assertEquals(daBia, daAna.getInvites().get(0)); // o convite da Bia não foi refeito

        service.definirConvites(ana, daAna, List.of("caio"));
        assertEquals(List.of("caio"), convidados());
        verify(collaborationRepository).delete(daBia);
        verify(notificationRepository).deleteByCollaboration(daBia);
    }

    @Test
    void listaVaziaDesfazOsPendentesENulaNaoMudaNada() {
        seguem(bia);
        service.definirConvites(ana, daAna, List.of("bia"));
        service.definirConvites(ana, daAna, null);
        assertEquals(List.of("bia"), convidados());
        service.definirConvites(ana, daAna, List.of());
        assertTrue(daAna.getInvites().isEmpty());
    }

    @Test
    void quemJaAceitouNaoSaiDaLista() {
        ReviewCollaboration c = aceita(bia, true);
        assertThrows(ConflictException.class, () -> service.definirConvites(ana, daAna, List.of()));
        seguem(caio);
        service.definirConvites(ana, daAna, List.of("bia", "caio")); // dá para marcar mais gente depois
        assertEquals(List.of("bia", "caio"), convidados());
        verify(collaborationRepository, never()).delete(c);
    }

    @Test
    void quemEntrouPorConviteNaoMarcaMaisNinguem() {
        Review daBia = aceita(bia, false).getInviteeReview();
        aceita(caio, false);
        service.definirConvites(bia, daBia, List.of("ana", "caio")); // repetir a sessão não é erro
        assertThrows(ConflictException.class, () -> service.definirConvites(bia, daBia, List.of("ana", "dani")));
    }

    @Test
    void edicaoDoTextoOriginalChegaATodasAsCopias() {
        ReviewCollaboration daBia = aceita(bia, true);
        ReviewCollaboration doCaio = aceita(caio, false);
        daAna.setText("Que filme! Vi de novo e mudei de ideia.");
        service.aoAtualizar(daAna);
        assertEquals(daAna.getText(), daBia.getInviteeReview().getText());
        assertEquals("Que filme!", doCaio.getInviteeReview().getText());
        assertTrue(daBia.isSharedText());
    }

    @Test
    void quemAceitouEscreveEAReviewViraDela() {
        ReviewCollaboration c = aceita(bia, true);
        Review daBia = c.getInviteeReview();
        daBia.setText("Na verdade achei mediano.");
        service.aoAtualizar(daBia);
        assertFalse(c.isSharedText());
        assertEquals("Que filme!", daAna.getText());
    }

    @Test
    void quemAceitouApagaESoElaSaiDaSessao() {
        ReviewCollaboration daBia = aceita(bia, false);
        ReviewCollaboration doCaio = aceita(caio, false);
        service.aoApagar(daBia.getInviteeReview());
        assertEquals(List.of("caio"), convidados());
        assertEquals(doCaio, doCaio.getInviteeReview().getCollaboration());
    }

    @Test
    void quemConvidouApagaEASessaoSeDesfaz() {
        ReviewCollaboration daBia = aceita(bia, false);
        ReviewCollaboration doCaio = aceita(caio, false);
        service.aoApagar(daAna);
        assertTrue(daAna.getInvites().isEmpty());
        assertNull(daBia.getInviteeReview().getCollaboration());
        assertNull(doCaio.getInviteeReview().getCollaboration());
    }

    @Test
    void cadaReviewMostraAsOutrasPessoasDaSessao() {
        seguem(caio);
        ReviewCollaboration daBia = aceita(bia, true);
        service.definirConvites(ana, daAna, List.of("bia", "caio")); // Caio ainda não respondeu

        ReviewDTO.Together vistoPelaAna = new ReviewDTO(daAna).getTogether();
        assertTrue(vistoPelaAna.invitedByMe());
        assertEquals(List.of("bia", "caio"), vistoPelaAna.partners().stream().map(p -> p.user().getUsername()).toList());
        assertNull(vistoPelaAna.partners().get(1).reviewId());

        ReviewDTO.Together vistoPelaBia = new ReviewDTO(daBia.getInviteeReview()).getTogether();
        assertFalse(vistoPelaBia.invitedByMe());
        assertTrue(vistoPelaBia.sharedText());
        assertEquals("ana", vistoPelaBia.invitedBy().getUsername());
        // o convite pendente do Caio não aparece para a Bia
        assertEquals(List.of("ana"), vistoPelaBia.partners().stream().map(p -> p.user().getUsername()).toList());
    }

    private ReviewCollaboration aceita(User quem, boolean textoCompartilhado) {
        ReviewCollaboration c = new ReviewCollaboration(daAna, quem);
        c.setId(ids.incrementAndGet());
        daAna.getInvites().add(c);
        service.concluirAceite(c, review(ids.incrementAndGet(), quem, daAna.getText()), textoCompartilhado);
        return c;
    }
}
