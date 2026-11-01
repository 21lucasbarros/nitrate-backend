package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.ReviewDTO;
import com.lucasbarros.nitrate.dto.ReviewRequestDTO;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.repositories.WatchlistRepository;
import com.lucasbarros.nitrate.services.exceptions.ConflictException;
import com.lucasbarros.nitrate.services.exceptions.ForbiddenException;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReviewService {

    private static final int TEXTO_MAX = 5000;

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private WatchlistRepository watchlistRepository;
    @Autowired private GraphExpansionService graphExpansionService;

    @Transactional
    public ReviewDTO criar(User user, ReviewRequestDTO request) {
        if (request.getMovieId() == null) {
            throw new IllegalArgumentException("Informe o filme (movieId)");
        }
        Movie filme = movieRepository.findById(request.getMovieId())
                .orElseThrow(() -> new ResourceNotFoundException("Filme não encontrado. Id: " + request.getMovieId()));

        if (reviewRepository.findByUserAndMovie(user, filme).isPresent()) {
            throw new ConflictException("Você já registrou esse filme. Edite a review existente.");
        }

        Review review = new Review(user, filme);
        aplicar(review, request);
        review = reviewRepository.save(review);

        // assistiu, então sai da watchlist
        watchlistRepository.findByUserAndMovie(user, filme).ifPresent(watchlistRepository::delete);

        expandirGrafoSeGostou(review);
        return new ReviewDTO(review);
    }

    @Transactional
    public ReviewDTO atualizar(User user, Long id, ReviewRequestDTO request) {
        Review review = buscarDoUsuario(user, id);
        aplicar(review, request);
        review.setUpdatedAt(Instant.now());
        expandirGrafoSeGostou(review);
        return new ReviewDTO(review);
    }

    @Transactional
    public void deletar(User user, Long id) {
        reviewRepository.delete(buscarDoUsuario(user, id));
    }

    @Transactional(readOnly = true)
    public List<ReviewDTO> recentes() {
        return paraDTO(reviewRepository.findTop20ByOrderByCreatedAtDesc());
    }

    @Transactional(readOnly = true)
    public List<ReviewDTO> doFilme(Long movieId) {
        Movie filme = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Filme não encontrado. Id: " + movieId));
        return paraDTO(reviewRepository.findByMovieOrderByCreatedAtDesc(filme));
    }

    // gostou do filme: o diretor e o elenco dele passam a alimentar o grafo (em segundo plano)
    private void expandirGrafoSeGostou(Review review) {
        boolean gostou = review.isLiked()
                || (review.getRating() != null && review.getRating() >= GraphExpansionService.NOTA_MINIMA_SEMENTE);
        if (gostou && !review.getMovie().isGrafoExpandido()) {
            graphExpansionService.expandir(review.getMovie().getId());
        }
    }

    private Review buscarDoUsuario(User user, Long id) {
        Review review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review não encontrada. Id: " + id));
        if (!review.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Você só pode alterar as suas próprias reviews.");
        }
        return review;
    }

    private void aplicar(Review review, ReviewRequestDTO request) {
        Double nota = request.getRating();
        if (nota != null && (nota < 0.5 || nota > 5 || nota * 2 != Math.floor(nota * 2))) {
            throw new IllegalArgumentException("A nota deve ir de 0.5 a 5, em passos de 0.5");
        }

        String texto = request.getText() == null ? null : request.getText().trim();
        if (texto != null && texto.isEmpty()) texto = null;
        if (texto != null && texto.length() > TEXTO_MAX) {
            throw new IllegalArgumentException("A review pode ter no máximo " + TEXTO_MAX + " caracteres");
        }

        LocalDate assistidoEm = request.getWatchedOn() == null ? LocalDate.now() : request.getWatchedOn();
        if (assistidoEm.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("A data em que assistiu não pode estar no futuro");
        }

        review.setRating(nota);
        review.setText(texto);
        review.setLiked(request.isLiked());
        // sem texto não há o que esconder
        review.setSpoiler(texto != null && request.isSpoiler());
        review.setWatchedOn(assistidoEm);
    }

    private List<ReviewDTO> paraDTO(List<Review> reviews) {
        List<ReviewDTO> dtos = new ArrayList<>();
        for (Review review : reviews) {
            dtos.add(new ReviewDTO(review));
        }
        return dtos;
    }
}
