package com.lucasbarros.nitrate.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * `seriesId` só é usado na criação; no PUT a série não muda (a temporada e o episódio podem mudar).
 * `episodeNumber` só vale junto com `seasonNumber`. `watchedWith`, como nos filmes, é a lista completa
 * de quem assistiu junto (usernames); nula não mexe nos convites.
 */
public record SeriesReviewRequestDTO(
        Long seriesId,
        Integer seasonNumber,
        Integer episodeNumber,
        Double rating,
        String text,
        boolean liked,
        boolean spoiler,
        boolean rewatch,
        LocalDate watchedOn,
        List<String> watchedWith) {

    /** os mesmos dados, sem mexer em quem assistiu junto (o aceite de um convite já define a sessão) */
    public SeriesReviewRequestDTO semConvites() {
        return new SeriesReviewRequestDTO(seriesId, seasonNumber, episodeNumber, rating, text, liked, spoiler,
                rewatch, watchedOn, null);
    }
}
