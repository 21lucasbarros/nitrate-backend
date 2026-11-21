package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.SeriesReview;
import com.lucasbarros.nitrate.entities.SeriesReviewLike;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SeriesReviewLikeRepository extends JpaRepository<SeriesReviewLike, Long> {
    long countByReview(SeriesReview review);
    boolean existsByReviewAndUser(SeriesReview review, User user);
    Optional<SeriesReviewLike> findByReviewAndUser(SeriesReview review, User user);

    @Modifying
    @Query("delete from SeriesReviewLike l where l.review = :review")
    void deleteAllByReview(@Param("review") SeriesReview review);

    // exclusão da conta: as curtidas que a pessoa deu e as que os registros dela receberam
    @Modifying
    @Query("delete from SeriesReviewLike l where l.user = :user " +
            "or l.review in (select r from SeriesReview r where r.user = :user)")
    void deleteAllInvolving(@Param("user") User user);
}
