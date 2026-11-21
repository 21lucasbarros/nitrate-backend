package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.ActivityDTO;
import com.lucasbarros.nitrate.dto.ActivityDTO.Type;
import com.lucasbarros.nitrate.dto.ListEngagementDTO;
import com.lucasbarros.nitrate.dto.MovieDTO;
import com.lucasbarros.nitrate.dto.MovieListDTO;
import com.lucasbarros.nitrate.dto.ReviewDTO;
import com.lucasbarros.nitrate.dto.UserSummaryDTO;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.MovieListRepository;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.repositories.ReviewCommentRepository;
import com.lucasbarros.nitrate.repositories.ReviewLikeRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.repositories.WatchlistRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * "Minha atividade" na página de um filme: tudo o que a pessoa fez com ele — registros (com nota,
 * curtida e review), watchlist, listas em que colocou o filme e reviews de outras pessoas que
 * curtiu ou comentou. Como no feed dos amigos, não há tabela de eventos: cada fonte entra com a
 * sua data. Por isso, o que foi desfeito (sair da watchlist, sair de uma lista) não aparece.
 */
@Service
public class MovieActivityService {

    private static final int COMENTARIO_PREVIA = 200;

    @Autowired private MovieRepository movieRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private WatchlistRepository watchlistRepository;
    @Autowired private MovieListRepository movieListRepository;
    @Autowired private ReviewLikeRepository reviewLikeRepository;
    @Autowired private ReviewCommentRepository reviewCommentRepository;

    @Transactional(readOnly = true)
    public List<ActivityDTO> atividade(User eu, Long movieId) {
        Movie filme = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Filme não encontrado. Id: " + movieId));
        UserSummaryDTO autor = new UserSummaryDTO(eu);
        List<ActivityDTO> itens = new ArrayList<>();

        List<Review> reviews = reviewRepository.findByUserAndMovieOrderByCreatedAtDesc(eu, filme);
        // curtidas que as minhas reviews com texto receberam, numa consulta só
        List<Long> comTexto = reviews.stream().filter(r -> r.getText() != null).map(Review::getId).toList();
        Map<Long, Long> curtidas = new HashMap<>();
        Set<Long> curtidasPorMim = new HashSet<>();
        if (!comTexto.isEmpty()) {
            reviewLikeRepository.contarPorReview(comTexto)
                    .forEach(l -> curtidas.put(((Number) l[0]).longValue(), ((Number) l[1]).longValue()));
            curtidasPorMim.addAll(reviewLikeRepository.curtidasPorMim(eu, comTexto));
        }
        for (Review r : reviews) {
            boolean texto = r.getText() != null;
            itens.add(new ActivityDTO(Type.REVIEW, "review-" + r.getId(), r.getCreatedAt(), autor,
                    new ReviewDTO(r), null, null, null, null,
                    texto ? curtidas.getOrDefault(r.getId(), 0L) : null,
                    texto ? curtidasPorMim.contains(r.getId()) : null));
        }

        watchlistRepository.findByUserAndMovie(eu, filme).ifPresent(w -> itens.add(new ActivityDTO(
                Type.WATCHLIST, "watchlist-" + w.getId(), w.getAddedAt(), autor,
                null, new MovieDTO(filme), null, null, null, null, null)));

        // entradas de antes do addedAt existir ficam com a data de criação da lista
        movieListRepository.findEntradasDoFilme(eu, filme).forEach(e -> itens.add(new ActivityDTO(
                Type.LIST_ENTRY, "entry-" + e.getId(),
                e.getAddedAt() != null ? e.getAddedAt() : e.getList().getCreatedAt(), autor,
                null, new MovieDTO(filme), MovieListDTO.resumo(e.getList(), ListEngagementDTO.VAZIO),
                null, null, null, null)));

        reviewLikeRepository.findCurtidasNoFilme(eu, filme).forEach(l -> itens.add(new ActivityDTO(
                Type.REVIEW_LIKE, "like-" + l.getId(), l.getCreatedAt(), autor,
                new ReviewDTO(l.getReview()), null, null, null, null, null, null)));

        reviewCommentRepository.findComentariosNoFilme(eu, filme).forEach(c -> itens.add(new ActivityDTO(
                Type.COMMENT, "comment-" + c.getId(), c.getCreatedAt(), autor,
                new ReviewDTO(c.getReview()), null, null, previa(c.getText()), null, null, null)));

        itens.sort(Comparator.comparing(ActivityDTO::at).reversed());
        return itens;
    }

    private static String previa(String texto) {
        return texto.length() > COMENTARIO_PREVIA ? texto.substring(0, COMENTARIO_PREVIA).trim() + "…" : texto;
    }
}
