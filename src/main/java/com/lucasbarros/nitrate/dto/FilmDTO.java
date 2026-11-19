package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.Review;

import java.time.LocalDate;
import java.util.List;

/**
 * Um filme da aba "Filmes" do perfil: o registro mais recente (nota, curtida, link) e todas as
 * vezes que a pessoa assistiu, do mais recente para o mais antigo, para escolher qual review abrir.
 */
public record FilmDTO(ReviewDTO review, List<Viewing> viewings) {

    /** uma vez que a pessoa assistiu: o suficiente para listar, sem o texto da review */
    public record Viewing(Long id, LocalDate watchedOn, Double rating, boolean liked, boolean rewatch, boolean hasText) {
        public Viewing(Review review) {
            this(review.getId(), review.getWatchedOn(), review.getRating(), review.isLiked(), review.isRewatch(),
                    review.getText() != null && !review.getText().isBlank());
        }
    }

    public FilmDTO(List<Review> registros) {
        this(new ReviewDTO(registros.get(0)), registros.stream().map(Viewing::new).toList());
    }
}
