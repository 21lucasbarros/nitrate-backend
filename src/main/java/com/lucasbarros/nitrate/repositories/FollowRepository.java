package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Follow;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowRepository extends JpaRepository<Follow, Long> {
    Optional<Follow> findByFollowerAndFollowed(User follower, User followed);
    boolean existsByFollowerAndFollowed(User follower, User followed);
    long countByFollower(User follower);
    long countByFollowed(User followed);
    List<Follow> findByFollowerOrderByCreatedAtDesc(User follower);
    List<Follow> findByFollowedOrderByCreatedAtDesc(User followed);
}
