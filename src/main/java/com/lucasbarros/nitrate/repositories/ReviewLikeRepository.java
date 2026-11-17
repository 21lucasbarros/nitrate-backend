package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.ReviewLike;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {
    long countByReview(Review review);
    boolean existsByReviewAndUser(Review review, User user);
    Optional<ReviewLike> findByReviewAndUser(Review review, User user);
    List<ReviewLike> findByUserOrderByCreatedAtDesc(User user);

    @Modifying
    @Query("delete from ReviewLike l where l.review = :review")
    void deleteAllByReview(@Param("review") Review review);

    // exclusão da conta: as curtidas que a pessoa deu e as que as reviews dela receberam
    @Modifying
    @Query("delete from ReviewLike l where l.user = :user " +
            "or l.review in (select r from Review r where r.user = :user)")
    void deleteAllInvolving(@Param("user") User user);
}
