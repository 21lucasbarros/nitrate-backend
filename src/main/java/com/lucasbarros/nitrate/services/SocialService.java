package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.MovieDTO;
import com.lucasbarros.nitrate.dto.ReviewDTO;
import com.lucasbarros.nitrate.dto.SimilarUserDTO;
import com.lucasbarros.nitrate.dto.UserSummaryDTO;
import com.lucasbarros.nitrate.entities.FavoriteFilm;
import com.lucasbarros.nitrate.entities.Follow;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Notification;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.FavoriteFilmRepository;
import com.lucasbarros.nitrate.repositories.FollowRepository;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.repositories.NotificationRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.security.RateLimiter;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import com.lucasbarros.nitrate.services.exceptions.TooManyRequestsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/** Seguir pessoas, o feed de quem você segue e os filmes favoritos do perfil. */
@Service
public class SocialService {

    public static final int MAX_FAVORITOS = 5;
    private static final int FEED_MAX = 30;
    private static final int BUSCA_MAX = 10;
    private static final int BUSCA_TERMO_MAX = 50;
    private static final long PARECIDOS_MINIMO_EM_COMUM = 3;
    private static final int PARECIDOS_MAX = 8;
    // seguir e deixar de seguir em loop mandaria uma notificação nova a cada vez
    private static final int SEGUIR_MAX = 100;
    private static final Duration JANELA_SEGUIR = Duration.ofHours(1);

    @Autowired private UserRepository userRepository;
    @Autowired private FollowRepository followRepository;
    @Autowired private FavoriteFilmRepository favoriteFilmRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private RateLimiter rateLimiter;

    // idempotente: seguir quem já segue não faz nada
    @Transactional
    public void seguir(User eu, String username) {
        User alvo = buscarPorUsername(username);
        if (alvo.getId().equals(eu.getId())) {
            throw new IllegalArgumentException("Você não pode seguir a si mesmo");
        }
        if (!followRepository.existsByFollowerAndFollowed(eu, alvo)) {
            if (!rateLimiter.tentar("seguir:" + eu.getId(), SEGUIR_MAX, JANELA_SEGUIR)) {
                throw new TooManyRequestsException("Você seguiu muita gente em pouco tempo. Espere um pouco e tente de novo.");
            }
            followRepository.save(new Follow(eu, alvo));
            notificationRepository.save(Notification.follow(alvo, eu));
        }
    }

    @Transactional
    public void deixarDeSeguir(User eu, String username) {
        User alvo = buscarPorUsername(username);
        followRepository.findByFollowerAndFollowed(eu, alvo).ifPresent(follow -> {
            followRepository.delete(follow);
            notificationRepository.deleteByUserAndActorAndType(alvo, eu, Notification.Type.FOLLOW);
        });
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDTO> seguidores(String username) {
        List<UserSummaryDTO> dtos = new ArrayList<>();
        for (Follow f : followRepository.findByFollowedOrderByCreatedAtDesc(buscarPorUsername(username))) {
            dtos.add(new UserSummaryDTO(f.getFollower()));
        }
        return dtos;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDTO> seguindo(String username) {
        List<UserSummaryDTO> dtos = new ArrayList<>();
        for (Follow f : followRepository.findByFollowerOrderByCreatedAtDesc(buscarPorUsername(username))) {
            dtos.add(new UserSummaryDTO(f.getFollowed()));
        }
        return dtos;
    }

    /** o que as pessoas que eu sigo registraram por último */
    @Transactional(readOnly = true)
    public List<ReviewDTO> feed(User eu, int limite) {
        int tamanho = Math.min(Math.max(1, limite), FEED_MAX);
        List<ReviewDTO> dtos = new ArrayList<>();
        reviewRepository.findFeed(eu, PageRequest.of(0, tamanho)).forEach(r -> dtos.add(new ReviewDTO(r)));
        return dtos;
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDTO> pesquisarUsuarios(String termo) {
        String limpo = termo == null ? "" : termo.toLowerCase().trim().replaceFirst("^@", "");
        if (limpo.length() < 2) return List.of();
        if (limpo.length() > BUSCA_TERMO_MAX) limpo = limpo.substring(0, BUSCA_TERMO_MAX);
        // o termo já vai como parâmetro (sem SQL injection), mas % e _ ainda seriam curingas do LIKE:
        // "%" listaria todo mundo e "_" casaria qualquer letra. Escapados, valem como texto literal.
        String literal = limpo.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
        List<UserSummaryDTO> dtos = new ArrayList<>();
        userRepository.pesquisar(literal, PageRequest.of(0, BUSCA_MAX)).forEach(u -> dtos.add(new UserSummaryDTO(u)));
        return dtos;
    }

    /**
     * Quem tem gosto parecido: pessoas que avaliaram pelo menos alguns dos mesmos filmes com notas
     * próximas. Quem você já segue fica de fora (a ideia é encontrar gente nova).
     */
    @Transactional(readOnly = true)
    public List<SimilarUserDTO> gostosParecidos(User eu) {
        record Candidato(SimilarUserDTO dto, double pontuacao) {}
        List<Candidato> candidatos = new ArrayList<>();
        for (Object[] linha : reviewRepository.findAfinidades(eu, PARECIDOS_MINIMO_EM_COMUM)) {
            User outro = (User) linha[0];
            long emComum = ((Number) linha[1]).longValue();
            double diferenca = ((Number) linha[2]).doubleValue();
            if (followRepository.existsByFollowerAndFollowed(eu, outro)) continue;
            // notas vão de 0.5 a 5: a maior diferença possível é 4.5
            double afinidade = Math.max(0, 1 - diferenca / 4.5);
            // concordar em muitos filmes vale mais do que concordar em poucos
            double pontuacao = afinidade * Math.sqrt(emComum);
            candidatos.add(new Candidato(new SimilarUserDTO(outro, emComum, (int) Math.round(afinidade * 100)), pontuacao));
        }
        return candidatos.stream()
                .sorted((a, b) -> Double.compare(b.pontuacao(), a.pontuacao()))
                .limit(PARECIDOS_MAX)
                .map(Candidato::dto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovieDTO> favoritos(User user) {
        List<MovieDTO> dtos = new ArrayList<>();
        List<FavoriteFilm> favoritos = favoriteFilmRepository.findByUserOrderByPositionAsc(user);
        if (favoritos.isEmpty()) return dtos;
        // o pôster que a pessoa escolheu nas reviews também vale nos favoritos dela
        Map<Long, String> posteres = new HashMap<>();
        reviewRepository.findPosteresEscolhidos(user).forEach(l -> posteres.put((Long) l[0], (String) l[1]));
        for (FavoriteFilm f : favoritos) {
            MovieDTO dto = new MovieDTO(f.getMovie());
            String poster = posteres.get(f.getMovie().getId());
            if (poster != null) dto.setPosterPath(poster);
            dtos.add(dto);
        }
        return dtos;
    }

    /** substitui a lista inteira; a ordem recebida é a ordem do perfil */
    @Transactional
    public List<MovieDTO> definirFavoritos(User eu, List<Long> movieIds) {
        // repetidos contam uma vez só
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(movieIds == null ? List.of() : movieIds));
        if (ids.size() > MAX_FAVORITOS) {
            throw new IllegalArgumentException("Escolha no máximo " + MAX_FAVORITOS + " filmes favoritos");
        }
        // o delete limpa o contexto de persistência, então os filmes são carregados depois dele;
        // se algum id não existir, a transação desfaz o delete
        favoriteFilmRepository.deleteAllByUser(eu);
        for (int i = 0; i < ids.size(); i++) {
            Long id = ids.get(i);
            Movie filme = movieRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Filme não encontrado. Id: " + id));
            favoriteFilmRepository.save(new FavoriteFilm(eu, filme, i + 1));
        }
        return favoritos(eu);
    }

    @Transactional(readOnly = true)
    public long contarSeguidores(User user) { return followRepository.countByFollowed(user); }

    @Transactional(readOnly = true)
    public long contarSeguindo(User user) { return followRepository.countByFollower(user); }

    @Transactional(readOnly = true)
    public boolean segue(User eu, User alvo) { return followRepository.existsByFollowerAndFollowed(eu, alvo); }

    private User buscarPorUsername(String username) {
        return userRepository.findFirstByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + username));
    }
}
