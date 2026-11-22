package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.CollaborationDTO;
import com.lucasbarros.nitrate.entities.Notification;
import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.ReviewCollaboration;
import com.lucasbarros.nitrate.entities.ReviewCollaboration.Status;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.FollowRepository;
import com.lucasbarros.nitrate.repositories.NotificationRepository;
import com.lucasbarros.nitrate.repositories.ReviewCollaborationRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.services.exceptions.ConflictException;
import com.lucasbarros.nitrate.services.exceptions.ForbiddenException;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Objects;

/**
 * Review em conjunto ("assisti com"): o convite que sai da review de quem registrou, a resposta
 * de quem foi marcado e o que acontece com o par quando uma das reviews muda ou é apagada.
 * Criar a review de quem aceita fica no ReviewService, que chama este serviço para validar e ligar.
 */
@Service
public class ReviewCollaborationService {

    @Autowired private ReviewCollaborationRepository collaborationRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FollowRepository followRepository;

    /**
     * Aplica o "assisti com" do formulário na review de `autor`. `username` nulo não muda nada;
     * vazio desfaz o convite pendente (ou recusado); outro nome troca o convite pendente.
     * Depois de aceito, o par não muda mais por aqui: só apagando uma das reviews.
     */
    @Transactional
    public void definirConvite(User autor, Review review, String username) {
        if (username == null) return;
        String nome = username.trim().replaceFirst("^@", "");
        ReviewCollaboration atual = review.getCollaboration();

        if (atual != null && atual.getStatus() == Status.ACCEPTED) {
            User parceiro = atual.parceiroDe(review);
            if (!nome.equalsIgnoreCase(parceiro.getUsername())) {
                throw new ConflictException("Esta review já está em conjunto com @" + parceiro.getUsername()
                        + ". Para desfazer, uma de vocês precisa apagar a própria review.");
            }
            return;
        }
        if (atual != null && atual.getStatus() == Status.PENDING && nome.equalsIgnoreCase(atual.getInvitee().getUsername())) {
            return; // o mesmo convite de antes
        }
        if (atual != null) desfazer(atual);
        if (nome.isEmpty()) return;

        User convidado = userRepository.findFirstByUsernameIgnoreCase(nome)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: @" + nome));
        if (convidado.getId().equals(autor.getId())) {
            throw new IllegalArgumentException("Você não pode marcar a si mesmo como companhia.");
        }
        // marcar alguém manda notificação: só para quem segue a pessoa, assim ninguém recebe convite de estranhos
        if (!followRepository.existsByFollowerAndFollowed(convidado, autor)) {
            throw new IllegalArgumentException("Só dá para marcar quem segue você: @"
                    + convidado.getUsername() + " ainda não te segue.");
        }

        ReviewCollaboration c = collaborationRepository.save(new ReviewCollaboration(review, convidado));
        review.setCollaboration(c);
        notificationRepository.save(Notification.conviteDeReview(c));
    }

    /** o convite, para quem foi convidado (ou quem convidou) ver a review e responder */
    @Transactional(readOnly = true)
    public CollaborationDTO buscar(User eu, Long id) {
        ReviewCollaboration c = buscarEntidade(id);
        if (!c.getInvitee().getId().equals(eu.getId()) && !c.getAuthor().getId().equals(eu.getId())) {
            throw new ForbiddenException("Este convite não é seu.");
        }
        return new CollaborationDTO(c);
    }

    /** confere que `eu` pode aceitar o convite agora; quem cria a review é o ReviewService */
    @Transactional(readOnly = true)
    public ReviewCollaboration paraResponder(User eu, Long id) {
        ReviewCollaboration c = buscarEntidade(id);
        if (!c.getInvitee().getId().equals(eu.getId())) {
            throw new ForbiddenException("Este convite não é seu.");
        }
        if (c.getStatus() != Status.PENDING) {
            throw new ConflictException(c.getStatus() == Status.ACCEPTED
                    ? "Você já aceitou este convite." : "Você já recusou este convite.");
        }
        return c;
    }

    /** a review de quem aceitou já existe: liga as duas e avisa quem convidou */
    @Transactional
    public void concluirAceite(ReviewCollaboration c, Review reviewDoConvidado, boolean textoCompartilhado) {
        Instant agora = Instant.now();
        c.setInviteeReview(reviewDoConvidado);
        c.setStatus(Status.ACCEPTED);
        c.setSharedText(textoCompartilhado);
        c.setRespondedAt(agora);
        reviewDoConvidado.setCollaboration(c);
        collaborationRepository.save(c);
        notificationRepository.marcarComoLidas(c, Notification.Type.COLLAB_INVITE, agora);
        notificationRepository.save(Notification.conviteAceito(c));
    }

    @Transactional
    public void recusar(User eu, Long id) {
        ReviewCollaboration c = paraResponder(eu, id);
        Instant agora = Instant.now();
        c.setStatus(Status.DECLINED);
        c.setRespondedAt(agora);
        collaborationRepository.save(c);
        notificationRepository.marcarComoLidas(c, Notification.Type.COLLAB_INVITE, agora);
    }

    /**
     * Uma das reviews do par foi editada. Quem aceitou usando o texto de quem convidou recebe as
     * edições desse texto; se ela própria mudar o texto, a review passa a ser só dela.
     */
    @Transactional
    public void aoAtualizar(Review review) {
        ReviewCollaboration c = review.getCollaboration();
        if (c == null || c.getStatus() != Status.ACCEPTED || !c.isSharedText()) return;
        Review original = c.getAuthorReview();
        Review copia = c.getInviteeReview();
        if (review.equals(original)) {
            copia.setText(original.getText());
            copia.setSpoiler(original.isSpoiler());
            copia.setUpdatedAt(Instant.now());
        } else if (!Objects.equals(review.getText(), original.getText())) {
            c.setSharedText(false);
        }
    }

    /** a review vai ser apagada: o par se desfaz e a outra review continua, só que sozinha */
    @Transactional
    public void aoApagar(Review review) {
        if (review.getCollaboration() != null) desfazer(review.getCollaboration());
    }

    private void desfazer(ReviewCollaboration c) {
        c.getAuthorReview().setCollaboration(null);
        if (c.getInviteeReview() != null) c.getInviteeReview().setCollaboration(null);
        notificationRepository.deleteByCollaboration(c);
        collaborationRepository.delete(c);
        // já no banco: um convite novo para a mesma review esbarraria no unique de author_review_id,
        // porque o insert (IDENTITY) sai na hora e o delete só sairia no fim da transação
        collaborationRepository.flush();
    }

    private ReviewCollaboration buscarEntidade(Long id) {
        return collaborationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Convite não encontrado. Id: " + id));
    }
}
