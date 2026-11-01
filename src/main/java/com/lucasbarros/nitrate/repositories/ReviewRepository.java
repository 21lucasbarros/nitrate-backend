package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByUserOrderByCreatedAtDesc(User user);
    List<Review> findByMovieOrderByCreatedAtDesc(Movie movie);
    List<Review> findTop20ByOrderByCreatedAtDesc();
    Optional<Review> findByUserAndMovie(User user, Movie movie);
    long countByUser(User user);
    long countByUserAndTextIsNotNull(User user);
    long countByUserAndLikedTrue(User user);

    // filmes que alguém curtiu ou avaliou bem: as sementes do grafo
    @Query("select distinct r.movie.id from Review r where r.liked = true or r.rating >= :notaMinima")
    List<Long> findIdsDeFilmesSemente(@Param("notaMinima") double notaMinima);
}
