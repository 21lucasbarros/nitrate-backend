package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Follow;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerAndFollowed(User follower, User followed);
    boolean existsByFollowerAndFollowed(User follower, User followed);
    long countByFollower(User follower);
    long countByFollowed(User followed);
    List<Follow> findByFollowerOrderByCreatedAtDesc(User follower);
    List<Follow> findByFollowedOrderByCreatedAtDesc(User followed);

    // feed de atividade: quem eu ou quem eu sigo começamos a seguir
    @Query("select f from Follow f where (f.follower = :me or f.follower in " +
            "(select x.followed from Follow x where x.follower = :me)) " +
            "and f.createdAt < :antes order by f.createdAt desc, f.id desc")
    List<Follow> findAtividade(@Param("me") User me, @Param("antes") Instant antes, Pageable pageable);

    // exclusão de conta: some quem a pessoa seguia e quem seguia ela
    @Modifying
    @Query("delete from Follow f where f.follower = :user or f.followed = :user")
    void deleteAllInvolving(@Param("user") User user);
}
