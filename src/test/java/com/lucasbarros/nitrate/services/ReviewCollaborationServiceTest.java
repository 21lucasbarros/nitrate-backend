package com.lucasbarros.nitrate.services;

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

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

// review em conjunto: quem pode ser marcado, troca do convite e o texto compartilhado (sem banco)
@ExtendWith(MockitoExtension.class)
class ReviewCollaborationServiceTest {

    @Mock private ReviewCollaborationRepository collaborationRepository;
    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private FollowRepository followRepository;
    @InjectMocks private ReviewCollaborationService service;

    private User ana;
    private User bia;
    private Review daAna;

    @BeforeEach
    void configura() {
        ana = new User("Ana", "ana", "ana@x.com", "hash");
        ana.setId(1L);
        bia = new User("Bia", "bia", "bia@x.com", "hash");
        bia.setId(2L);
        daAna = review(10L, ana, "Que filme!");
        lenient().when(userRepository.findFirstByUsernameIgnoreCase("bia")).thenReturn(Optional.of(bia));
        lenient().when(collaborationRepository.save(any(ReviewCollaboration.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private static Review review(Long id, User user, String texto) {
        Review r = new Review(user, new Movie());
        r.setId(id);
        r.setText(texto);
        return r;
    }

    @Test
    void marcaQuemSegueEAvisaPeloSino() {
        when(followRepository.existsByFollowerAndFollowed(bia, ana)).thenReturn(true);
        service.definirConvite(ana, daAna, " @bia ");
        assertNotNull(daAna.getCollaboration());
        assertEquals(bia, daAna.getCollaboration().getInvitee());
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void naoMarcaQuemNaoSegue() {
        when(followRepository.existsByFollowerAndFollowed(bia, ana)).thenReturn(false);
        assertThrows(IllegalArgumentException.class, () -> service.definirConvite(ana, daAna, "bia"));
        assertNull(daAna.getCollaboration());
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void naoMarcaASiMesmo() {
        when(userRepository.findFirstByUsernameIgnoreCase("ana")).thenReturn(Optional.of(ana));
        assertThrows(IllegalArgumentException.class, () -> service.definirConvite(ana, daAna, "ana"));
    }

    @Test
    void vazioDesfazOConvitePendente() {
        ReviewCollaboration c = new ReviewCollaboration(daAna, bia);
        daAna.setCollaboration(c);
        service.definirConvite(ana, daAna, "");
        assertNull(daAna.getCollaboration());
        verify(collaborationRepository).delete(c);
        verify(notificationRepository).deleteByCollaboration(c);
    }

    @Test
    void depoisDeAceitoOParNaoTroca() {
        ReviewCollaboration c = aceita(true);
        assertThrows(ConflictException.class, () -> service.definirConvite(ana, daAna, ""));
        service.definirConvite(ana, daAna, "bia"); // repetir o mesmo par não é erro
        verify(collaborationRepository, never()).delete(c);
    }

    @Test
    void edicaoDoTextoOriginalChegaACopia() {
        ReviewCollaboration c = aceita(true);
        daAna.setText("Que filme! Vi de novo e mudei de ideia.");
        service.aoAtualizar(daAna);
        assertEquals(daAna.getText(), c.getInviteeReview().getText());
        assertTrue(c.isSharedText());
    }

    @Test
    void quemAceitouEscreveEAReviewViraDela() {
        ReviewCollaboration c = aceita(true);
        Review daBia = c.getInviteeReview();
        daBia.setText("Na verdade achei mediano.");
        service.aoAtualizar(daBia);
        assertFalse(c.isSharedText());
        assertEquals("Que filme!", daAna.getText());
    }

    private ReviewCollaboration aceita(boolean textoCompartilhado) {
        ReviewCollaboration c = new ReviewCollaboration(daAna, bia);
        daAna.setCollaboration(c);
        service.concluirAceite(c, review(20L, bia, daAna.getText()), textoCompartilhado);
        return c;
    }
}
