package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.entities.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    List<UserAchievement> findByUserOrderByAchievedAtDesc(User user);
    List<UserAchievement> findByUserAndHiddenFalseOrderByAchievedAtDesc(User user);
    Optional<UserAchievement> findByUserAndTypeAndPersonTmdbId(User user, String type, Long personTmdbId);

    @Modifying
    @Query("delete from UserAchievement a where a.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
