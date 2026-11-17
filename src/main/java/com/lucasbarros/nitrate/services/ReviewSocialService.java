package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.ReviewCommentDTO;
import com.lucasbarros.nitrate.dto.ReviewEngagementDTO;
import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.ReviewComment;
import com.lucasbarros.nitrate.entities.ReviewLike;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.ReviewCommentRepository;
import com.lucasbarros.nitrate.repositories.ReviewLikeRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.services.exceptions.ForbiddenException;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** Curtir e comentar a review de outra pessoa. */
@Service
public class ReviewSocialService {

    public static final int COMENTARIO_MAX = 1000;

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private ReviewLikeRepository reviewLikeRepository;
    @Autowired private ReviewCommentRepository reviewCommentRepository;
    @Autowired private UserRepository userRepository;

    @Transactional(readOnly = true)
    public ReviewEngagementDTO engajamento(Long reviewId, User quemVe) {
        return engajamento(buscarReview(reviewId), quemVe);
    }

    // idempotente: curtir de novo não duplica
    @Transactional
    public ReviewEngagementDTO curtir(User eu, Long reviewId) {
        Review review = buscarReview(reviewId);
        if (review.getUser().getId().equals(eu.getId())) {
            throw new IllegalArgumentException("Você não pode curtir a sua própria review.");
        }
        if (!reviewLikeRepository.existsByReviewAndUser(review, eu)) {
            reviewLikeRepository.save(new ReviewLike(review, userRepository.getReferenceById(eu.getId())));
        }
        return engajamento(review, eu);
    }

    @Transactional
    public ReviewEngagementDTO descurtir(User eu, Long reviewId) {
        Review review = buscarReview(reviewId);
        reviewLikeRepository.findByReviewAndUser(review, eu).ifPresent(reviewLikeRepository::delete);
        return engajamento(review, eu);
    }

    @Transactional(readOnly = true)
    public List<ReviewCommentDTO> comentarios(Long reviewId) {
        return reviewCommentRepository.findByReviewOrderByCreatedAtAscIdAsc(buscarReview(reviewId)).stream()
                .map(ReviewCommentDTO::new)
                .toList();
    }

    @Transactional
    public ReviewCommentDTO comentar(User eu, Long reviewId, String texto) {
        String limpo = texto == null ? "" : texto.trim();
        if (limpo.isEmpty()) throw new IllegalArgumentException("Escreva alguma coisa no comentário.");
        if (limpo.length() > COMENTARIO_MAX) {
            throw new IllegalArgumentException("O comentário pode ter até " + COMENTARIO_MAX + " caracteres.");
        }
        Review review = buscarReview(reviewId);
        ReviewComment comentario = new ReviewComment(review, userRepository.getReferenceById(eu.getId()), limpo);
        return new ReviewCommentDTO(reviewCommentRepository.save(comentario));
    }

    /** quem escreveu o comentário ou o dono da review podem apagar */
    @Transactional
    public void excluirComentario(User eu, Long reviewId, Long comentarioId) {
        ReviewComment comentario = reviewCommentRepository.findById(comentarioId)
                .filter(c -> c.getReview().getId().equals(reviewId))
                .orElseThrow(() -> new ResourceNotFoundException("Comentário não encontrado. Id: " + comentarioId));
        boolean autor = comentario.getUser().getId().equals(eu.getId());
        boolean donoDaReview = comentario.getReview().getUser().getId().equals(eu.getId());
        if (!autor && !donoDaReview) {
            throw new ForbiddenException("Só quem escreveu o comentário ou o dono da review podem apagá-lo.");
        }
        reviewCommentRepository.delete(comentario);
    }

    /** a review vai ser apagada: curtidas e comentários vão junto */
    @Transactional
    public void limparReview(Review review) {
        reviewLikeRepository.deleteAllByReview(review);
        reviewCommentRepository.deleteAllByReview(review);
    }

    private ReviewEngagementDTO engajamento(Review review, User quemVe) {
        return new ReviewEngagementDTO(
                reviewLikeRepository.countByReview(review),
                quemVe == null ? null : reviewLikeRepository.existsByReviewAndUser(review, quemVe),
                reviewCommentRepository.countByReview(review));
    }

    private Review buscarReview(Long id) {
        return reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review não encontrada. Id: " + id));
    }
}
