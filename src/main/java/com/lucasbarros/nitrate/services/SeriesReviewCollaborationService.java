package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.SeriesCollaborationDTO;
import com.lucasbarros.nitrate.entities.Notification;
import com.lucasbarros.nitrate.entities.ReviewCollaboration.Status;
import com.lucasbarros.nitrate.entities.SeriesReview;
import com.lucasbarros.nitrate.entities.SeriesReviewCollaboration;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.NotificationRepository;
import com.lucasbarros.nitrate.repositories.SeriesReviewCollaborationRepository;
import com.lucasbarros.nitrate.services.exceptions.ConflictException;
import com.lucasbarros.nitrate.services.exceptions.ForbiddenException;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Registro de série em conjunto ("assisti com"), com as mesmas regras do ReviewCollaborationService
 * dos filmes: um convite por pessoa marcada (até MAX_PESSOAS, só quem segue quem marcou), a resposta
 * de cada pessoa e o que acontece com a sessão quando um dos registros muda ou é apagado. Criar o
 * registro de quem aceita fica no SeriesDiaryService.
 */
@Service
public class SeriesReviewCollaborationService {

    @Autowired private SeriesReviewCollaborationRepository collaborationRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private ReviewCollaborationService reviewCollaborationService;

    /**
     * Aplica o "assisti com" do formulário no registro de `autor`: `usernames` é a lista completa de
     * quem assistiu junto. Nula não muda nada; quem sai da lista com o convite pendente (ou recusado)
     * tem o convite desfeito, e quem entra recebe um convite novo.
     */
    @Transactional
    public void definirConvites(User autor, SeriesReview review, List<String> usernames) {
        if (usernames == null) return;
        Set<String> nomes = ReviewCollaborationService.normalizar(usernames);

        // quem entrou por convite não marca mais ninguém: a sessão é de quem convidou
        if (review.getCollaboration() != null) {
            Set<String> daSessao = new LinkedHashSet<>();
            for (User u : participantes(review.reviewDeOrigem())) {
                if (!u.getId().equals(autor.getId())) daSessao.add(u.getUsername().toLowerCase(Locale.ROOT));
            }
            if (!daSessao.containsAll(nomes)) {
                throw new ConflictException("Só @" + review.getCollaboration().getAuthor().getUsername()
                        + ", que marcou você, pode marcar mais gente neste registro em conjunto.");
            }
            return;
        }

        for (SeriesReviewCollaboration c : new ArrayList<>(review.getInvites())) {
            String nome = c.getInvitee().getUsername().toLowerCase(Locale.ROOT);
            boolean continua = nomes.remove(nome);
            if (c.getStatus() == Status.ACCEPTED && !continua) {
                throw new ConflictException("@" + c.getInvitee().getUsername() + " já aceitou o registro em conjunto. "
                        + "Para sair dele, alguém de vocês precisa apagar o próprio registro.");
            }
            // recusado e marcado de novo: convite novo no lugar do antigo
            if (!continua || c.getStatus() == Status.DECLINED) {
                desfazer(c);
                if (continua) nomes.add(nome);
            }
        }

        for (User convidado : reviewCollaborationService.convidaveis(autor, nomes)) {
            SeriesReviewCollaboration c = collaborationRepository.save(new SeriesReviewCollaboration(review, convidado));
            review.getInvites().add(c);
            notificationRepository.save(Notification.conviteDeReview(c));
        }
    }

    /** o convite, para quem foi convidado (ou quem convidou) ver o registro e responder */
    @Transactional(readOnly = true)
    public SeriesCollaborationDTO buscar(User eu, Long id) {
        SeriesReviewCollaboration c = buscarEntidade(id);
        if (!c.getInvitee().getId().equals(eu.getId()) && !c.getAuthor().getId().equals(eu.getId())) {
            throw new ForbiddenException("Este convite não é seu.");
        }
        return new SeriesCollaborationDTO(c);
    }

    /** confere que `eu` pode aceitar o convite agora; quem cria o registro é o SeriesDiaryService */
    @Transactional(readOnly = true)
    public SeriesReviewCollaboration paraResponder(User eu, Long id) {
        SeriesReviewCollaboration c = buscarEntidade(id);
        if (!c.getInvitee().getId().equals(eu.getId())) {
            throw new ForbiddenException("Este convite não é seu.");
        }
        if (c.getStatus() != Status.PENDING) {
            throw new ConflictException(c.getStatus() == Status.ACCEPTED
                    ? "Você já aceitou este convite." : "Você já recusou este convite.");
        }
        return c;
    }

    /** o registro de quem aceitou já existe: liga ao de quem convidou e avisa quem convidou */
    @Transactional
    public void concluirAceite(SeriesReviewCollaboration c, SeriesReview doConvidado, boolean textoCompartilhado) {
        Instant agora = Instant.now();
        c.setInviteeReview(doConvidado);
        c.setStatus(Status.ACCEPTED);
        c.setSharedText(textoCompartilhado);
        c.setRespondedAt(agora);
        doConvidado.setCollaboration(c);
        collaborationRepository.save(c);
        notificationRepository.marcarComoLidas(c, Notification.Type.COLLAB_INVITE, agora);
        notificationRepository.save(Notification.conviteAceito(c));
    }

    @Transactional
    public void recusar(User eu, Long id) {
        SeriesReviewCollaboration c = paraResponder(eu, id);
        Instant agora = Instant.now();
        c.setStatus(Status.DECLINED);
        c.setRespondedAt(agora);
        collaborationRepository.save(c);
        notificationRepository.marcarComoLidas(c, Notification.Type.COLLAB_INVITE, agora);
    }

    /**
     * Um dos registros da sessão foi editado. Quem aceitou usando o texto de quem convidou recebe as
     * edições desse texto; se ela própria mudar o texto, o registro passa a ser só dela.
     */
    @Transactional
    public void aoAtualizar(SeriesReview review) {
        SeriesReviewCollaboration recebido = review.getCollaboration();
        if (recebido != null) {
            if (recebido.getStatus() == Status.ACCEPTED && recebido.isSharedText()
                    && !Objects.equals(review.getText(), recebido.getAuthorReview().getText())) {
                recebido.setSharedText(false);
            }
            return;
        }
        for (SeriesReviewCollaboration c : review.getInvites()) {
            if (c.getStatus() != Status.ACCEPTED || !c.isSharedText()) continue;
            SeriesReview copia = c.getInviteeReview();
            copia.setText(review.getText());
            copia.setSpoiler(review.isSpoiler());
            copia.setUpdatedAt(Instant.now());
        }
    }

    /**
     * O registro vai ser apagado. Se é de quem aceitou, só ele sai da sessão; se é de quem convidou,
     * a sessão se desfaz. Os outros registros continuam, só que sozinhos.
     */
    @Transactional
    public void aoApagar(SeriesReview review) {
        if (review.getCollaboration() != null) desfazer(review.getCollaboration());
        for (SeriesReviewCollaboration c : new ArrayList<>(review.getInvites())) desfazer(c);
    }

    /** quem convidou e quem aceitou, nessa ordem (convites pendentes e recusados não entram) */
    public static List<User> participantes(SeriesReview origem) {
        List<User> todos = new ArrayList<>();
        todos.add(origem.getUser());
        for (SeriesReviewCollaboration c : origem.getInvites()) {
            if (c.getStatus() == Status.ACCEPTED) todos.add(c.getInvitee());
        }
        return todos;
    }

    private void desfazer(SeriesReviewCollaboration c) {
        c.getAuthorReview().getInvites().remove(c);
        if (c.getInviteeReview() != null) c.getInviteeReview().setCollaboration(null);
        notificationRepository.deleteBySeriesCollaboration(c);
        collaborationRepository.delete(c);
        // já no banco: um convite novo para a mesma pessoa esbarraria no unique de (author_review_id, invitee_id)
        collaborationRepository.flush();
    }

    private SeriesReviewCollaboration buscarEntidade(Long id) {
        return collaborationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Convite não encontrado. Id: " + id));
    }
}
