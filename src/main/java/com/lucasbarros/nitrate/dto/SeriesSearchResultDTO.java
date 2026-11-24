package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.Series;
import com.lucasbarros.nitrate.tmdb.dto.TmdbTvSummaryDTO;

/** Série vinda do TMDB (busca e listas). `id` e `slug` só existem se ela já foi importada. */
public record SeriesSearchResultDTO(
        Long id,
        String slug,
        Long tmdbId,
        String name,
        Integer firstAirYear,
        String posterPath,
        String overview,
        Double rating) {

    public SeriesSearchResultDTO(TmdbTvSummaryDTO resumo, Series local) {
        this(local == null ? null : local.getId(),
                local == null ? null : local.getSlug(),
                resumo.getId(),
                resumo.getName(),
                ano(resumo.getFirstAirDate()),
                resumo.getPosterPath(),
                resumo.getOverview() == null || resumo.getOverview().isBlank() ? null : resumo.getOverview(),
                resumo.getVoteAverage());
    }

    /** série que já está no Nitrate (catálogo filtrado) */
    public SeriesSearchResultDTO(Series local) {
        this(local.getId(), local.getSlug(), local.getTmdbId(), local.getName(), local.getFirstAirYear(),
                local.getPosterPath(), local.getOverview(), local.getRating());
    }

    /** "2008-01-20" → 2008; nulo se a data vier vazia */
    public static Integer ano(String data) {
        if (data == null || data.length() < 4) return null;
        try {
            return Integer.parseInt(data.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
