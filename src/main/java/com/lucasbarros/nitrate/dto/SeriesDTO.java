package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.Series;

import java.util.List;

/** Uma série do Nitrate (o que fica no banco). Detalhes ao vivo do TMDB ficam em SeriesDetailsDTO. */
public record SeriesDTO(
        Long id,
        Long tmdbId,
        String slug,
        String name,
        Integer firstAirYear,
        Integer lastAirYear, // nulo se ainda está no ar
        String status,
        Integer numberOfSeasons,
        Integer numberOfEpisodes,
        Double rating,       // nota do TMDB, de 0 a 10
        String creators,
        List<String> genres,
        String posterPath,
        String overview) {

    public SeriesDTO(Series s) {
        this(s.getId(), s.getTmdbId(), s.getSlug(), s.getName(), s.getFirstAirYear(), s.getLastAirYear(),
                s.getStatus(), s.getNumberOfSeasons(), s.getNumberOfEpisodes(), s.getRating(), s.getCreators(),
                List.copyOf(s.getGenres()), s.getPosterPath(), s.getOverview());
    }
}
