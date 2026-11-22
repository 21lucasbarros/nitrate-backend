package com.lucasbarros.nitrate.dto;

import java.time.LocalDate;

/**
 * `seriesId` só é usado na criação; no PUT a série não muda (a temporada e o episódio podem mudar).
 * `episodeNumber` só vale junto com `seasonNumber`.
 */
public record SeriesReviewRequestDTO(
        Long seriesId,
        Integer seasonNumber,
        Integer episodeNumber,
        Double rating,
        String text,
        boolean liked,
        boolean spoiler,
        LocalDate watchedOn) {
}
