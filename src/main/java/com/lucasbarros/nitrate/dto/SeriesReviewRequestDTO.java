package com.lucasbarros.nitrate.dto;

import java.time.LocalDate;

/** `seriesId` só é usado na criação; no PUT a série não muda (a temporada pode mudar). */
public record SeriesReviewRequestDTO(
        Long seriesId,
        Integer seasonNumber,
        Double rating,
        String text,
        boolean liked,
        boolean spoiler,
        LocalDate watchedOn) {
}
