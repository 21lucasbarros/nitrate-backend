package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.ReviewComment;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReviewCommentRepository extends JpaRepository<ReviewComment, Long> {
    List<ReviewComment> findByReviewOrderByCreatedAtAscIdAsc(Review review);
    long countByReview(Review review);
    List<ReviewComment> findByUserOrderByCreatedAtDesc(User user);

    @Modifying
    @Query("delete from ReviewComment c where c.review = :review")
    void deleteAllByReview(@Param("review") Review review);

    // exclusão da conta: os comentários que a pessoa fez e os que as reviews dela receberam
    @Modifying
    @Query("delete from ReviewComment c where c.user = :user " +
            "or c.review in (select r from Review r where r.user = :user)")
    void deleteAllInvolving(@Param("user") User user);
}
