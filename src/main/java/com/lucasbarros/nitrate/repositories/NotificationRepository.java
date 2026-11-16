package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Notification;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserOrderByCreatedAtDescIdDesc(User user, Pageable pageable);
    long countByUserAndReadAtIsNull(User user);
    boolean existsByUserAndMovieAndType(User user, Movie movie, Notification.Type type);

    @Modifying
    @Query("update Notification n set n.readAt = :agora where n.user = :user and n.readAt is null")
    int marcarTodasComoLidas(@Param("user") User user, @Param("agora") Instant agora);

    @Modifying
    @Query("delete from Notification n where n.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
