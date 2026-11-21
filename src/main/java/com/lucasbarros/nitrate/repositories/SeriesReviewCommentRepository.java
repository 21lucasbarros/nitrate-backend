package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.SeriesReview;
import com.lucasbarros.nitrate.entities.SeriesReviewComment;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SeriesReviewCommentRepository extends JpaRepository<SeriesReviewComment, Long> {
    List<SeriesReviewComment> findByReviewOrderByCreatedAtAscIdAsc(SeriesReview review);
    long countByReview(SeriesReview review);

    @Modifying
    @Query("delete from SeriesReviewComment c where c.review = :review")
    void deleteAllByReview(@Param("review") SeriesReview review);

    // exclusão da conta: os comentários que a pessoa fez e os que os registros dela receberam
    @Modifying
    @Query("delete from SeriesReviewComment c where c.user = :user " +
            "or c.review in (select r from SeriesReview r where r.user = :user)")
    void deleteAllInvolving(@Param("user") User user);
}
