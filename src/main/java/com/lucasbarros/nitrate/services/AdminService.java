package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.AdminStatsDTO;
import com.lucasbarros.nitrate.dto.UserSummaryDTO;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.FollowRepository;
import com.lucasbarros.nitrate.repositories.ListCommentRepository;
import com.lucasbarros.nitrate.repositories.ListLikeRepository;
import com.lucasbarros.nitrate.repositories.MovieListRepository;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.repositories.ReviewCommentRepository;
import com.lucasbarros.nitrate.repositories.ReviewLikeRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.repositories.UserBadgeRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.repositories.WatchlistRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** números do painel de admin */
@Service
public class AdminService {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final int DIAS_GRAFICO = 30;
    private static final int TAMANHO_LISTAS = 10;

    @Autowired private UserRepository userRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private ReviewLikeRepository reviewLikeRepository;
    @Autowired private ReviewCommentRepository reviewCommentRepository;
    @Autowired private MovieListRepository movieListRepository;
    @Autowired private ListLikeRepository listLikeRepository;
    @Autowired private ListCommentRepository listCommentRepository;
    @Autowired private FollowRepository followRepository;
    @Autowired private WatchlistRepository watchlistRepository;
    @Autowired private UserBadgeRepository userBadgeRepository;
    @Autowired private EntityManager entityManager;

    @Transactional(readOnly = true)
    public AdminStatsDTO stats() {
        Instant agora = Instant.now();
        Instant dia = agora.minus(Duration.ofDays(1));
        Instant semana = agora.minus(Duration.ofDays(7));
        Instant mes = agora.minus(Duration.ofDays(30));

        AdminStatsDTO.Users users = new AdminStatsDTO.Users(
                userRepository.count(),
                userRepository.countByCreatedAtAfter(semana),
                userRepository.countByCreatedAtAfter(mes),
                userRepository.countByLastSeenAtAfter(dia),
                userRepository.countByLastSeenAtAfter(semana),
                userRepository.countByLastSeenAtAfter(mes));

        AdminStatsDTO.Content content = new AdminStatsDTO.Content(
                reviewRepository.count(),
                reviewRepository.countByCreatedAtAfter(semana),
                reviewRepository.countByCreatedAtAfter(mes),
                reviewRepository.countByTextIsNotNull(),
                movieRepository.count(),
                reviewLikeRepository.count(),
                reviewCommentRepository.count(),
                movieListRepository.count(),
                listLikeRepository.count(),
                listCommentRepository.count(),
                followRepository.count(),
                watchlistRepository.count(),
                userBadgeRepository.count());

        List<AdminStatsDTO.RecentUser> recentes = userRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, TAMANHO_LISTAS)).stream()
                .map(u -> new AdminStatsDTO.RecentUser(new UserSummaryDTO(u), u.getCreatedAt(), u.getLastSeenAt(),
                        reviewRepository.countByUser(u)))
                .toList();

        List<AdminStatsDTO.TopUser> maisAtivos = reviewRepository
                .findMaisAtivos(mes, PageRequest.of(0, TAMANHO_LISTAS)).stream()
                .map(linha -> new AdminStatsDTO.TopUser(new UserSummaryDTO((User) linha[0]), (Long) linha[1]))
                .toList();

        return new AdminStatsDTO(users, content, dias(agora), recentes, maisAtivos);
    }

    /**
     * Últimos DIAS_GRAFICO dias, do mais antigo para hoje. Agrupado aqui, e não no SQL, para o dia
     * seguir o fuso de São Paulo sem depender de como o Postgres guardou o timestamp.
     */
    private List<AdminStatsDTO.Day> dias(Instant agora) {
        LocalDate hoje = LocalDate.ofInstant(agora, FUSO);
        LocalDate primeiro = hoje.minusDays(DIAS_GRAFICO - 1);
        Instant desde = primeiro.atStartOfDay(FUSO).toInstant();

        Map<LocalDate, Long> cadastros = new HashMap<>();
        entityManager.createQuery("select u.createdAt from User u where u.createdAt >= :desde", Instant.class)
                .setParameter("desde", desde)
                .getResultList()
                .forEach(t -> cadastros.merge(LocalDate.ofInstant(t, FUSO), 1L, Long::sum));

        Map<LocalDate, Long> registros = new HashMap<>();
        Map<LocalDate, Set<Long>> pessoas = new HashMap<>();
        entityManager.createQuery("select r.createdAt, r.user.id from Review r where r.createdAt >= :desde", Object[].class)
                .setParameter("desde", desde)
                .getResultList()
                .forEach(linha -> {
                    LocalDate data = LocalDate.ofInstant((Instant) linha[0], FUSO);
                    registros.merge(data, 1L, Long::sum);
                    pessoas.computeIfAbsent(data, d -> new HashSet<>()).add((Long) linha[1]);
                });

        List<AdminStatsDTO.Day> dias = new ArrayList<>(DIAS_GRAFICO);
        for (LocalDate d = primeiro; !d.isAfter(hoje); d = d.plusDays(1)) {
            dias.add(new AdminStatsDTO.Day(d, cadastros.getOrDefault(d, 0L), registros.getOrDefault(d, 0L),
                    pessoas.getOrDefault(d, Set.of()).size()));
        }
        return dias;
    }
}
