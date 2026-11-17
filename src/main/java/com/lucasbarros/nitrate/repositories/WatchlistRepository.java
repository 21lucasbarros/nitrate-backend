package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.entities.WatchlistItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WatchlistRepository extends JpaRepository<WatchlistItem, Long> {
    List<WatchlistItem> findByUserOrderByAddedAtDesc(User user);
    Optional<WatchlistItem> findByUserAndMovie(User user, Movie movie);
    boolean existsByUserAndMovie(User user, Movie movie);
    long countByUser(User user);
    List<WatchlistItem> findByMovie(Movie movie);

    // feed de atividade: filmes que eu ou quem eu sigo pusemos na watchlist
    @Query("select w from WatchlistItem w where (w.user = :me or w.user in " +
            "(select f.followed from Follow f where f.follower = :me)) " +
            "and w.addedAt < :antes order by w.addedAt desc, w.id desc")
    List<WatchlistItem> findAtividade(@Param("me") User me, @Param("antes") Instant antes, Pageable pageable);

    // Radar de Estreias: filmes em alguma watchlist que ainda podem estrear em algum lugar
    // (sem data, com estreia recente ou futura) e que ainda não chegaram ao streaming
    @Query("select distinct w.movie.id from WatchlistItem w " +
            "where (w.movie.radarNoStreaming is null or w.movie.radarNoStreaming = false) " +
            "and (w.movie.releaseDate is null or w.movie.releaseDate >= :desde)")
    List<Long> findIdsParaRadar(@Param("desde") LocalDate desde);

    @Modifying
    @Query("delete from WatchlistItem w where w.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
