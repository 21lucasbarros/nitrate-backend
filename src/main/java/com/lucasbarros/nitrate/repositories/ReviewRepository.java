package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByUserOrderByCreatedAtDesc(User user);
    List<Review> findByMovieOrderByCreatedAtDesc(Movie movie);
    List<Review> findTop20ByOrderByCreatedAtDesc();
    // destaque na landing: reviews com texto e sem spoiler
    List<Review> findTop3ByTextIsNotNullAndSpoilerFalseOrderByCreatedAtDesc();
    List<Review> findByUserAndWatchedOnBetweenOrderByWatchedOnAsc(User user, LocalDate inicio, LocalDate fim);

    @Query("select r.watchedOn from Review r where r.user = :user and r.watchedOn is not null")
    List<LocalDate> findDatasAssistidas(@Param("user") User user);
    Optional<Review> findByUserAndMovie(User user, Movie movie);
    long countByUser(User user);
    long countByUserAndTextIsNotNull(User user);
    long countByUserAndLikedTrue(User user);

    // filmes que alguém curtiu ou avaliou bem e que ainda não tiveram o grafo expandido
    @Query("select distinct r.movie.id from Review r " +
            "where (r.liked = true or r.rating >= :notaMinima) and r.movie.grafoExpandido = false")
    List<Long> findIdsDeSementesPendentes(@Param("notaMinima") double notaMinima);
}
