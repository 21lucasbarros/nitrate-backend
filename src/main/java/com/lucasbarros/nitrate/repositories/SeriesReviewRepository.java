package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Series;
import com.lucasbarros.nitrate.entities.SeriesReview;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SeriesReviewRepository extends JpaRepository<SeriesReview, Long> {
    List<SeriesReview> findBySeriesOrderByCreatedAtDesc(Series series);
    List<SeriesReview> findByUserOrderByCreatedAtDesc(User user);
    List<SeriesReview> findByUserAndSeriesOrderByWatchedOnDescCreatedAtDesc(User user, Series series);
    List<SeriesReview> findByUserAndWatchedOnBetweenOrderByWatchedOnAsc(User user, LocalDate inicio, LocalDate fim);

    @Query("select r.watchedOn from SeriesReview r where r.user = :user and r.watchedOn is not null")
    List<LocalDate> findDatasAssistidas(@Param("user") User user);

    @Modifying
    @Query("delete from SeriesReview r where r.user = :user")
    void deleteAllByUser(@Param("user") User user);
}
