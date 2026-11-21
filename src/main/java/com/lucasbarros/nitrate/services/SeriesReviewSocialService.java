package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.ReviewCommentDTO;
import com.lucasbarros.nitrate.dto.ReviewEngagementDTO;
import com.lucasbarros.nitrate.dto.UserSummaryDTO;
import com.lucasbarros.nitrate.entities.SeriesReview;
import com.lucasbarros.nitrate.entities.SeriesReviewComment;
import com.lucasbarros.nitrate.entities.SeriesReviewLike;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.SeriesReviewCommentRepository;
import com.lucasbarros.nitrate.repositories.SeriesReviewLikeRepository;
import com.lucasbarros.nitrate.repositories.SeriesReviewRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.services.exceptions.ForbiddenException;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Curtir e comentar o registro de série de outra pessoa (as mesmas regras das reviews de filme). */
@Service
public class SeriesReviewSocialService {

    @Autowired private SeriesReviewRepository seriesReviewRepository;
    @Autowired private SeriesReviewLikeRepository likeRepository;
    @Autowired private SeriesReviewCommentRepository commentRepository;
    @Autowired private UserRepository userRepository;

    @Transactional(readOnly = true)
    public ReviewEngagementDTO engajamento(Long reviewId, User quemVe) {
        return engajamento(buscarReview(reviewId), quemVe);
    }

    // idempotente: curtir de novo não duplica
    @Transactional
    public ReviewEngagementDTO curtir(User eu, Long reviewId) {
        SeriesReview review = buscarReview(reviewId);
        if (review.getUser().getId().equals(eu.getId())) {
            throw new IllegalArgumentException("Você não pode curtir a sua própria review.");
        }
        if (!likeRepository.existsByReviewAndUser(review, eu)) {
            likeRepository.save(new SeriesReviewLike(review, userRepository.getReferenceById(eu.getId())));
        }
        return engajamento(review, eu);
    }

    @Transactional
    public ReviewEngagementDTO descurtir(User eu, Long reviewId) {
        SeriesReview review = buscarReview(reviewId);
        likeRepository.findByReviewAndUser(review, eu).ifPresent(likeRepository::delete);
        return engajamento(review, eu);
    }

    @Transactional(readOnly = true)
    public List<ReviewCommentDTO> comentarios(Long reviewId) {
        return commentRepository.findByReviewOrderByCreatedAtAscIdAsc(buscarReview(reviewId)).stream()
                .map(SeriesReviewSocialService::paraDTO)
                .toList();
    }

    @Transactional
    public ReviewCommentDTO comentar(User eu, Long reviewId, String texto) {
        String limpo = texto == null ? "" : texto.trim();
        if (limpo.isEmpty()) throw new IllegalArgumentException("Escreva alguma coisa no comentário.");
        if (limpo.length() > ReviewSocialService.COMENTARIO_MAX) {
            throw new IllegalArgumentException("O comentário pode ter até " + ReviewSocialService.COMENTARIO_MAX + " caracteres.");
        }
        SeriesReview review = buscarReview(reviewId);
        SeriesReviewComment comentario = new SeriesReviewComment(review, userRepository.getReferenceById(eu.getId()), limpo);
        return paraDTO(commentRepository.save(comentario));
    }

    /** quem escreveu o comentário ou o dono do registro podem apagar */
    @Transactional
    public void excluirComentario(User eu, Long reviewId, Long comentarioId) {
        SeriesReviewComment comentario = commentRepository.findById(comentarioId)
                .filter(c -> c.getReview().getId().equals(reviewId))
                .orElseThrow(() -> new ResourceNotFoundException("Comentário não encontrado. Id: " + comentarioId));
        boolean autor = comentario.getUser().getId().equals(eu.getId());
        boolean donoDaReview = comentario.getReview().getUser().getId().equals(eu.getId());
        if (!autor && !donoDaReview) {
            throw new ForbiddenException("Só quem escreveu o comentário ou o dono da review podem apagá-lo.");
        }
        commentRepository.delete(comentario);
    }

    /** o registro vai ser apagado: curtidas e comentários vão junto */
    @Transactional
    public void limparReview(SeriesReview review) {
        likeRepository.deleteAllByReview(review);
        commentRepository.deleteAllByReview(review);
    }

    private ReviewEngagementDTO engajamento(SeriesReview review, User quemVe) {
        return new ReviewEngagementDTO(
                likeRepository.countByReview(review),
                quemVe == null ? null : likeRepository.existsByReviewAndUser(review, quemVe),
                commentRepository.countByReview(review));
    }

    // mesmo formato dos comentários das reviews de filme: o front usa o mesmo componente
    private static ReviewCommentDTO paraDTO(SeriesReviewComment c) {
        return new ReviewCommentDTO(c.getId(), new UserSummaryDTO(c.getUser()), c.getText(), c.getCreatedAt());
    }

    private SeriesReview buscarReview(Long id) {
        return seriesReviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review não encontrada. Id: " + id));
    }
}
