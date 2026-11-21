package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Review;
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
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByUserOrderByCreatedAtDesc(User user);
    Page<Review> findByUser(User user, Pageable pageable);
    Page<Review> findByUserAndTextIsNotNull(User user, Pageable pageable);
    List<Review> findByMovieOrderByCreatedAtDesc(Movie movie);
    List<Review> findAllByOrderByCreatedAtDesc(Pageable pageable);
    // feed: o que as pessoas que `user` segue registraram por último
    @Query("select r from Review r where r.user in " +
            "(select f.followed from Follow f where f.follower = :user) " +
            "order by r.createdAt desc, r.id desc")
    List<Review> findFeed(@Param("user") User user, Pageable pageable);
    // feed de atividade ("Amigos"): registros meus e de quem eu sigo, antes de `antes`
    @Query("select r from Review r where (r.user = :me or r.user in " +
            "(select f.followed from Follow f where f.follower = :me)) " +
            "and r.createdAt < :antes order by r.createdAt desc, r.id desc")
    List<Review> findAtividade(@Param("me") User me, @Param("antes") Instant antes, Pageable pageable);
    // destaque na landing: reviews com texto e sem spoiler
    List<Review> findTop3ByTextIsNotNullAndSpoilerFalseOrderByCreatedAtDesc();
    List<Review> findByUserAndWatchedOnBetweenOrderByWatchedOnAsc(User user, LocalDate inicio, LocalDate fim);

    @Query("select r.watchedOn from Review r where r.user = :user and r.watchedOn is not null")
    List<LocalDate> findDatasAssistidas(@Param("user") User user);
    // a review mais recente do filme (pode haver várias: reassistidos)
    Optional<Review> findFirstByUserAndMovieOrderByWatchedOnDescCreatedAtDesc(User user, Movie movie);
    boolean existsByUserAndMovie(User user, Movie movie);
    // atividade de uma pessoa com um filme: todos os registros dela
    List<Review> findByUserAndMovieOrderByCreatedAtDesc(User user, Movie movie);
    long countByUserAndMovie(User user, Movie movie);

    // filmes diferentes no diário (reassistir não conta duas vezes)
    @Query("select count(distinct r.movie) from Review r where r.user = :user")
    long countFilmesDistintos(@Param("user") User user);
    long countByUserAndTextIsNotNull(User user);
    long countByUserAndLikedTrue(User user);

    // quantas vezes cada filme foi registrado no Nitrate: a "popularidade" usada no onboarding
    // (o filme não guarda a popularidade do TMDB). Linhas: [Long movieId, Long registros]
    @Query("select r.movie.id, count(r) from Review r group by r.movie.id")
    List<Object[]> countPorFilme();

    // painel de admin
    long countByCreatedAtAfter(Instant desde);
    long countByTextIsNotNull();
    long countByUser(User user);
    // quem mais registrou filmes desde `desde`. Linhas: [User, Long registros]
    @Query("select r.user, count(r) from Review r where r.createdAt >= :desde " +
            "group by r.user order by count(r) desc")
    List<Object[]> findMaisAtivos(@Param("desde") Instant desde, Pageable pageable);

    // filmes que alguém curtiu ou avaliou bem e que ainda não tiveram o grafo expandido
    @Query("select distinct r.movie.id from Review r " +
            "where (r.liked = true or r.rating >= :notaMinima) and r.movie.grafoExpandido = false")
    List<Long> findIdsDeSementesPendentes(@Param("notaMinima") double notaMinima);

    // gostos parecidos: para cada pessoa, quantos filmes ela avaliou em comum com `user`
    // e a diferença média entre as notas. Linhas: [User, Long filmesEmComum, Double diferencaMedia]
    @Query("select r2.user, count(distinct r2.movie), avg(abs(r1.rating - r2.rating)) " +
            "from Review r1, Review r2 " +
            "where r1.user = :user and r2.movie = r1.movie and r2.user <> :user " +
            "and r1.rating is not null and r2.rating is not null " +
            "group by r2.user " +
            "having count(distinct r2.movie) >= :minimoEmComum")
    List<Object[]> findAfinidades(@Param("user") User user, @Param("minimoEmComum") long minimoEmComum);

    // descoberta pela comunidade: filmes mais registrados desde `desde` (cada pessoa conta uma vez).
    // Linhas: [Long movieId, Long pessoas]
    @Query("select r.movie.id, count(distinct r.user) from Review r " +
            "where r.createdAt >= :desde group by r.movie.id order by count(distinct r.user) desc, r.movie.id desc")
    List<Object[]> findMaisRegistrados(@Param("desde") Instant desde, Pageable pageable);

    // descoberta pela comunidade: maiores notas médias entre filmes avaliados por pelo menos `minimo` pessoas.
    // Linhas: [Long movieId, Double media]
    @Query("select r.movie.id, avg(r.rating) from Review r where r.rating is not null " +
            "group by r.movie.id having count(distinct r.user) >= :minimo " +
            "order by avg(r.rating) desc, count(distinct r.user) desc")
    List<Object[]> findMaisBemAvaliados(@Param("minimo") long minimo, Pageable pageable);

    // filmografia de uma pessoa: nota média e quantas pessoas avaliaram cada filme.
    // Linhas: [Long tmdbId, Double media, Long pessoas]
    @Query("select r.movie.tmdbId, avg(r.rating), count(distinct r.user) from Review r " +
            "where r.rating is not null and r.movie.tmdbId in :tmdbIds group by r.movie.tmdbId")
    List<Object[]> findMediasPorTmdbId(@Param("tmdbIds") Collection<Long> tmdbIds);

    // quais destes filmes (ids do TMDB) a pessoa já registrou: progresso na filmografia de alguém
    @Query("select distinct r.movie.tmdbId from Review r where r.user = :user and r.movie.tmdbId in :tmdbIds")
    List<Long> findTmdbIdsAssistidos(@Param("user") User user, @Param("tmdbIds") Collection<Long> tmdbIds);

    @Modifying
    @Query("delete from Review r where r.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
