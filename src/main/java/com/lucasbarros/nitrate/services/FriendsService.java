package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.ActivityDTO;
import com.lucasbarros.nitrate.dto.ActivityDTO.Type;
import com.lucasbarros.nitrate.dto.ActivityPageDTO;
import com.lucasbarros.nitrate.dto.MovieDTO;
import com.lucasbarros.nitrate.dto.ListEngagementDTO;
import com.lucasbarros.nitrate.dto.MovieListDTO;
import com.lucasbarros.nitrate.dto.ReviewDTO;
import com.lucasbarros.nitrate.dto.UserSummaryDTO;
import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.FollowRepository;
import com.lucasbarros.nitrate.repositories.MovieListRepository;
import com.lucasbarros.nitrate.repositories.ReviewCommentRepository;
import com.lucasbarros.nitrate.repositories.ReviewLikeRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.repositories.WatchlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * O feed da página "Amigos": o que eu e quem eu sigo fizemos — registros, filmes guardados na
 * watchlist, listas públicas criadas, reviews curtidas e comentadas, gente nova seguida.
 * Não há tabela de eventos: cada fonte devolve os seus mais recentes antes do cursor, e o feed
 * junta tudo pela data.
 */
@Service
public class FriendsService {

    private static final int TAMANHO_MAX = 50;
    private static final int COMENTARIO_PREVIA = 200;

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private WatchlistRepository watchlistRepository;
    @Autowired private MovieListRepository movieListRepository;
    @Autowired private ReviewLikeRepository reviewLikeRepository;
    @Autowired private ReviewCommentRepository reviewCommentRepository;
    @Autowired private FollowRepository followRepository;

    @Transactional(readOnly = true)
    public ActivityPageDTO feed(User eu, Instant antes, int tamanho) {
        int n = Math.min(Math.max(1, tamanho), TAMANHO_MAX);
        Instant cursor = antes == null ? Instant.now().plusSeconds(60) : antes;
        // cada fonte traz até n; juntas, as n mais recentes são o que vale para esta página
        PageRequest pagina = PageRequest.of(0, n);
        List<ActivityDTO> itens = new ArrayList<>();

        List<Review> reviews = reviewRepository.findAtividade(eu, cursor, pagina);
        // curtidas das reviews com texto: duas consultas para a página toda, e não duas por review
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
            itens.add(new ActivityDTO(Type.REVIEW, "review-" + r.getId(), r.getCreatedAt(), new UserSummaryDTO(r.getUser()),
                    new ReviewDTO(r), null, null, null, null,
                    texto ? curtidas.getOrDefault(r.getId(), 0L) : null,
                    texto ? curtidasPorMim.contains(r.getId()) : null));
        }
        watchlistRepository.findAtividade(eu, cursor, pagina).forEach(w -> itens.add(new ActivityDTO(
                Type.WATCHLIST, "watchlist-" + w.getId(), w.getAddedAt(), new UserSummaryDTO(w.getUser()),
                null, new MovieDTO(w.getMovie()), null, null, null, null, null)));
        movieListRepository.findAtividade(eu, cursor, pagina).forEach(l -> itens.add(new ActivityDTO(
                Type.LIST, "list-" + l.getId(), l.getCreatedAt(), new UserSummaryDTO(l.getUser()),
                null, null, MovieListDTO.resumo(l, ListEngagementDTO.VAZIO), null, null, null, null)));
        reviewLikeRepository.findAtividade(eu, cursor, pagina).forEach(l -> itens.add(new ActivityDTO(
                Type.REVIEW_LIKE, "like-" + l.getId(), l.getCreatedAt(), new UserSummaryDTO(l.getUser()),
                new ReviewDTO(l.getReview()), null, null, null, null, null, null)));
        reviewCommentRepository.findAtividade(eu, cursor, pagina).forEach(c -> itens.add(new ActivityDTO(
                Type.COMMENT, "comment-" + c.getId(), c.getCreatedAt(), new UserSummaryDTO(c.getUser()),
                new ReviewDTO(c.getReview()), null, null, previa(c.getText()), null, null, null)));
        followRepository.findAtividade(eu, cursor, pagina).forEach(f -> itens.add(new ActivityDTO(
                Type.FOLLOW, "follow-" + f.getId(), f.getCreatedAt(), new UserSummaryDTO(f.getFollower()),
                null, null, null, null, new UserSummaryDTO(f.getFollowed()), null, null)));

        itens.sort(Comparator.comparing(ActivityDTO::at).reversed());
        List<ActivityDTO> pagina1 = itens.size() > n ? itens.subList(0, n) : itens;
        // a próxima página começa logo antes do último item mostrado
        Instant proximo = pagina1.size() < n ? null : pagina1.get(pagina1.size() - 1).at();
        return new ActivityPageDTO(List.copyOf(pagina1), proximo);
    }

    private static String previa(String texto) {
        return texto.length() > COMENTARIO_PREVIA ? texto.substring(0, COMENTARIO_PREVIA).trim() + "…" : texto;
    }
}
