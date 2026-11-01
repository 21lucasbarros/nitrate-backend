package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
