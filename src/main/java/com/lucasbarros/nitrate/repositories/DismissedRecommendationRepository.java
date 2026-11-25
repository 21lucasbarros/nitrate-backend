package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.DismissedRecommendation;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DismissedRecommendationRepository extends JpaRepository<DismissedRecommendation, Long> {
    boolean existsByUserAndMovie(User user, Movie movie);

    // os filmes dispensados, do mais recente para o mais antigo
    @Query("select d.movie.id from DismissedRecommendation d where d.user = :user order by d.createdAt desc")
    List<Long> findMovieIdsByUser(@Param("user") User user);

    @Modifying
    @Query("delete from DismissedRecommendation d where d.user = :user and d.movie.id = :movieId")
    int deleteByUserAndMovieId(@Param("user") User user, @Param("movieId") Long movieId);

    @Modifying
    @Query("delete from DismissedRecommendation d where d.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
