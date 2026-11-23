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
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Review em conjunto ("assisti com"): os convites que saem da review de quem registrou (um por
 * pessoa marcada, até MAX_PESSOAS), a resposta de cada pessoa e o que acontece com a sessão quando uma
 * das reviews muda ou é apagada. Criar a review de quem aceita fica no ReviewService, que chama
 * este serviço para validar e ligar.
 */
@Service
public class ReviewCollaborationService {

    /** quantas pessoas cabem no "assisti com", sem contar quem registrou */
    public static final int MAX_PESSOAS = 15;

    @Autowired private ReviewCollaborationRepository collaborationRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private FollowRepository followRepository;

    /**
     * Aplica o "assisti com" do formulário na review de `autor`: `usernames` é a lista completa de
     * quem assistiu junto. Nula não muda nada; quem sai da lista com o convite pendente (ou
     * recusado) tem o convite desfeito, e quem entra recebe um convite novo. Quem já aceitou não
     * sai por aqui: só apagando a própria review (ou quem convidou apagando a dela).
     */
    @Transactional
    public void definirConvites(User autor, Review review, List<String> usernames) {
        if (usernames == null) return;
        Set<String> nomes = normalizar(usernames);

        // quem entrou por convite não marca mais ninguém: a sessão é de quem convidou
        if (review.getCollaboration() != null) {
            Set<String> daSessao = new LinkedHashSet<>();
            for (User u : participantes(review.reviewDeOrigem())) {
                if (!u.getId().equals(autor.getId())) daSessao.add(u.getUsername().toLowerCase(Locale.ROOT));
            }
            if (!daSessao.containsAll(nomes)) {
                throw new ConflictException("Só @" + review.getCollaboration().getAuthor().getUsername()
                        + ", que marcou você, pode marcar mais gente nesta review em conjunto.");
            }
            return;
        }

        for (ReviewCollaboration c : new ArrayList<>(review.getInvites())) {
            String nome = c.getInvitee().getUsername().toLowerCase(Locale.ROOT);
            boolean continua = nomes.remove(nome);
            if (c.getStatus() == Status.ACCEPTED && !continua) {
                throw new ConflictException("@" + c.getInvitee().getUsername() + " já aceitou a review em conjunto. "
                        + "Para sair dela, alguém de vocês precisa apagar a própria review.");
            }
            // recusado e marcado de novo: convite novo no lugar do antigo
            if (!continua || c.getStatus() == Status.DECLINED) {
                desfazer(c);
                if (continua) nomes.add(nome);
            }
        }

        for (User convidado : convidaveis(autor, nomes)) {
            ReviewCollaboration c = collaborationRepository.save(new ReviewCollaboration(review, convidado));
            review.getInvites().add(c);
            notificationRepository.save(Notification.conviteDeReview(c));
        }
    }

    /**
     * Os usernames do "assisti com", sem @, em minúsculas e sem repetir. A lista é a sessão inteira
     * (quem já aceitou precisa estar nela), então o tamanho dela é o total. Vale também para as séries.
     */
    public static Set<String> normalizar(List<String> usernames) {
        Set<String> nomes = new LinkedHashSet<>();
        for (String u : usernames) {
            String nome = u == null ? "" : u.trim().replaceFirst("^@", "").toLowerCase(Locale.ROOT);
            if (!nome.isEmpty()) nomes.add(nome);
        }
        if (nomes.size() > MAX_PESSOAS) {
            throw new IllegalArgumentException("Dá para marcar até " + MAX_PESSOAS + " pessoas numa review em conjunto.");
        }
        return nomes;
    }

    /** confere todo mundo que vai receber convite antes de mandar qualquer um (vale também para as séries) */
    public List<User> convidaveis(User autor, Set<String> nomes) {
        List<User> convidados = new ArrayList<>();
        for (String nome : nomes) {
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
            convidados.add(convidado);
        }
        return convidados;
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

    /** a review de quem aceitou já existe: liga à de quem convidou e avisa quem convidou */
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
     * Uma das reviews da sessão foi editada. Quem aceitou usando o texto de quem convidou recebe as
     * edições desse texto; se ela própria mudar o texto, a review passa a ser só dela.
     */
    @Transactional
    public void aoAtualizar(Review review) {
        ReviewCollaboration recebido = review.getCollaboration();
        if (recebido != null) {
            if (recebido.getStatus() == Status.ACCEPTED && recebido.isSharedText()
                    && !Objects.equals(review.getText(), recebido.getAuthorReview().getText())) {
                recebido.setSharedText(false);
            }
            return;
        }
        for (ReviewCollaboration c : review.getInvites()) {
            if (c.getStatus() != Status.ACCEPTED || !c.isSharedText()) continue;
            Review copia = c.getInviteeReview();
            copia.setText(review.getText());
            copia.setSpoiler(review.isSpoiler());
            copia.setUpdatedAt(Instant.now());
        }
    }

    /**
     * A review vai ser apagada. Se é de quem aceitou, só ela sai da sessão; se é de quem convidou,
     * a sessão se desfaz. As outras reviews continuam, só que sozinhas.
     */
    @Transactional
    public void aoApagar(Review review) {
        if (review.getCollaboration() != null) desfazer(review.getCollaboration());
        for (ReviewCollaboration c : new ArrayList<>(review.getInvites())) desfazer(c);
    }

    /** quem convidou e quem aceitou, nessa ordem (convites pendentes e recusados não entram) */
    public static List<User> participantes(Review origem) {
        List<User> todos = new ArrayList<>();
        todos.add(origem.getUser());
        for (ReviewCollaboration c : origem.getInvites()) {
            if (c.getStatus() == Status.ACCEPTED) todos.add(c.getInvitee());
        }
        return todos;
    }

    private void desfazer(ReviewCollaboration c) {
        c.getAuthorReview().getInvites().remove(c);
        if (c.getInviteeReview() != null) c.getInviteeReview().setCollaboration(null);
        notificationRepository.deleteByCollaboration(c);
        collaborationRepository.delete(c);
        // já no banco: um convite novo para a mesma pessoa esbarraria no unique de (author_review_id, invitee_id),
        // porque o insert (IDENTITY) sai na hora e o delete só sairia no fim da transação
        collaborationRepository.flush();
    }

    private ReviewCollaboration buscarEntidade(Long id) {
        return collaborationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Convite não encontrado. Id: " + id));
    }
}
