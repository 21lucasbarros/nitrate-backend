package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Notification;
import com.lucasbarros.nitrate.entities.Series;
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
    boolean existsByUserAndSeriesAndType(User user, Series series, Notification.Type type);
    boolean existsByUserAndSeriesAndTypeAndSeasonNumber(User user, Series series, Notification.Type type, Integer seasonNumber);

    @Modifying
    @Query("update Notification n set n.readAt = :agora where n.user = :user and n.readAt is null")
    int marcarTodasComoLidas(@Param("user") User user, @Param("agora") Instant agora);

    // deixar de seguir apaga o aviso: seguir e desseguir várias vezes não enche o sino
    @Modifying
    @Query("delete from Notification n where n.user = :user and n.actor = :actor and n.type = :type")
    void deleteByUserAndActorAndType(@Param("user") User user, @Param("actor") User actor,
                                     @Param("type") Notification.Type type);

    // exclusão de conta: os avisos da pessoa e os que ela gerou para outros
    @Modifying
    @Query("delete from Notification n where n.user = :user or n.actor = :user")
    void deleteAllByUser(@Param("user") User user);
}
