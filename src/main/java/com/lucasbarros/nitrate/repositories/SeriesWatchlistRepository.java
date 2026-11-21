package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Series;
import com.lucasbarros.nitrate.entities.SeriesWatchlistItem;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeriesWatchlistRepository extends JpaRepository<SeriesWatchlistItem, Long> {
    List<SeriesWatchlistItem> findByUserOrderByAddedAtDesc(User user);
    Optional<SeriesWatchlistItem> findByUserAndSeries(User user, Series series);
    boolean existsByUserAndSeries(User user, Series series);

    @Modifying
    @Query("delete from SeriesWatchlistItem w where w.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
