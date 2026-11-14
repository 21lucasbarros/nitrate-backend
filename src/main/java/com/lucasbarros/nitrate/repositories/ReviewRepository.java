package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
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
    // destaque na landing: reviews com texto e sem spoiler
    List<Review> findTop3ByTextIsNotNullAndSpoilerFalseOrderByCreatedAtDesc();
    List<Review> findByUserAndWatchedOnBetweenOrderByWatchedOnAsc(User user, LocalDate inicio, LocalDate fim);

    @Query("select r.watchedOn from Review r where r.user = :user and r.watchedOn is not null")
    List<LocalDate> findDatasAssistidas(@Param("user") User user);
    // a review mais recente do filme (pode haver várias: reassistidos)
    Optional<Review> findFirstByUserAndMovieOrderByWatchedOnDescCreatedAtDesc(User user, Movie movie);
    boolean existsByUserAndMovie(User user, Movie movie);
    long countByUserAndMovie(User user, Movie movie);

    // filmes diferentes no diário (reassistir não conta duas vezes)
    @Query("select count(distinct r.movie) from Review r where r.user = :user")
    long countFilmesDistintos(@Param("user") User user);
    long countByUserAndTextIsNotNull(User user);
    long countByUserAndLikedTrue(User user);

    // filmes que alguém curtiu ou avaliou bem e que ainda não tiveram o grafo expandido
    @Query("select distinct r.movie.id from Review r " +
            "where (r.liked = true or r.rating >= :notaMinima) and r.movie.grafoExpandido = false")
    List<Long> findIdsDeSementesPendentes(@Param("notaMinima") double notaMinima);
}
