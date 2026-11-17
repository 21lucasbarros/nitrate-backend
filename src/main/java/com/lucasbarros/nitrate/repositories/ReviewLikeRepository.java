package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.ReviewLike;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {
    long countByReview(Review review);
    boolean existsByReviewAndUser(Review review, User user);
    Optional<ReviewLike> findByReviewAndUser(Review review, User user);
    List<ReviewLike> findByUserOrderByCreatedAtDesc(User user);

    // feed: curtidas de várias reviews de uma vez. Linhas: [Long reviewId, Long curtidas]
    @Query("select l.review.id, count(l) from ReviewLike l where l.review.id in :ids group by l.review.id")
    List<Object[]> contarPorReview(@Param("ids") List<Long> ids);

    // feed: quais dessas reviews eu curti
    @Query("select l.review.id from ReviewLike l where l.user = :me and l.review.id in :ids")
    List<Long> curtidasPorMim(@Param("me") User me, @Param("ids") List<Long> ids);

    // feed de atividade: reviews que eu ou quem eu sigo curtimos
    @Query("select l from ReviewLike l where (l.user = :me or l.user in " +
            "(select f.followed from Follow f where f.follower = :me)) " +
            "and l.createdAt < :antes order by l.createdAt desc, l.id desc")
    List<ReviewLike> findAtividade(@Param("me") User me, @Param("antes") Instant antes, Pageable pageable);

    @Modifying
    @Query("delete from ReviewLike l where l.review = :review")
    void deleteAllByReview(@Param("review") Review review);

    // exclusão da conta: as curtidas que a pessoa deu e as que as reviews dela receberam
    @Modifying
    @Query("delete from ReviewLike l where l.user = :user " +
            "or l.review in (select r from Review r where r.user = :user)")
    void deleteAllInvolving(@Param("user") User user);
}
