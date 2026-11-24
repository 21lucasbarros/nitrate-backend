package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Series;
import com.lucasbarros.nitrate.entities.SeriesReview;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface SeriesReviewRepository extends JpaRepository<SeriesReview, Long> {

    // descoberta pela comunidade, como nos filmes: as séries que mais gente registrou desde `desde`.
    // Linhas: [Long seriesId, Long pessoas]
    @Query("select r.series.id, count(distinct r.user) from SeriesReview r " +
            "where r.createdAt >= :desde group by r.series.id order by count(distinct r.user) desc, r.series.id desc")
    List<Object[]> findMaisRegistradas(@Param("desde") Instant desde, Pageable pageable);

    List<SeriesReview> findBySeriesOrderByCreatedAtDesc(Series series);
    List<SeriesReview> findByUserOrderByCreatedAtDesc(User user);
    List<SeriesReview> findByUserAndSeriesOrderByWatchedOnDescCreatedAtDesc(User user, Series series);
    List<SeriesReview> findByUserAndWatchedOnBetweenOrderByWatchedOnAsc(User user, LocalDate inicio, LocalDate fim);
    List<SeriesReview> findAllByOrderByCreatedAtDesc(Pageable pageable);
    // diário do perfil, junto com os filmes
    Page<SeriesReview> findByUser(User user, Pageable pageable);
    Page<SeriesReview> findByUserAndTextIsNotNull(User user, Pageable pageable);
    long countByUser(User user);
    long countByUserAndTextIsNotNull(User user);
    // quantas séries diferentes a pessoa registrou (a aba Séries do perfil)
    @Query("select count(distinct r.series) from SeriesReview r where r.user = :user")
    long countSeriesDistintas(@Param("user") User user);
    // feed: o que as pessoas que `user` segue registraram por último, como o dos filmes
    @Query("select r from SeriesReview r where r.user in " +
            "(select f.followed from Follow f where f.follower = :user) " +
            "order by r.createdAt desc, r.id desc")
    List<SeriesReview> findFeed(@Param("user") User user, Pageable pageable);

    // Radar de Estreias: quem já registrou a série (ou uma temporada dela) e vai querer saber da próxima
    // nota média da série inteira (sem as de temporada e episódio), como a dos filmes na página da pessoa.
    // Linhas: [Long tmdbId, Double media, Long pessoas]
    @Query("select r.series.tmdbId, avg(r.rating), count(distinct r.user) from SeriesReview r " +
            "where r.rating is not null and r.seasonNumber is null and r.series.tmdbId in :tmdbIds group by r.series.tmdbId")
    List<Object[]> findMediasPorTmdbId(@Param("tmdbIds") Collection<Long> tmdbIds);

    @Query("select distinct r.user from SeriesReview r where r.series = :series")
    List<User> findUsuariosQueRegistraram(@Param("series") Series series);

    @Query("select r.watchedOn from SeriesReview r where r.user = :user and r.watchedOn is not null")
    List<LocalDate> findDatasAssistidas(@Param("user") User user);

    @Modifying
    @Query("delete from SeriesReview r where r.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
