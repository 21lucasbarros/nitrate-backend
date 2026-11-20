package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.entities.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    List<UserBadge> findByUser(User user);
    Optional<UserBadge> findByUserAndType(User user, String type);
    List<UserBadge> findAllByOrderByAwardedAtDesc();

    @Modifying
    @Query("delete from UserBadge b where b.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
