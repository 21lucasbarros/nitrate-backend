package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.SeriesReview;

import java.time.Instant;
import java.time.LocalDate;

public record SeriesReviewDTO(
        Long id,
        SeriesDTO series,
        UserSummaryDTO user,
        Integer seasonNumber, // nulo = a série inteira
        Double rating,
        String text,
        boolean liked,
        boolean spoiler,
        LocalDate watchedOn,
        Instant createdAt) {

    public SeriesReviewDTO(SeriesReview r) {
        this(r.getId(), new SeriesDTO(r.getSeries()), new UserSummaryDTO(r.getUser()), r.getSeasonNumber(),
                r.getRating(), r.getText(), r.isLiked(), r.isSpoiler(), r.getWatchedOn(), r.getCreatedAt());
    }
}
