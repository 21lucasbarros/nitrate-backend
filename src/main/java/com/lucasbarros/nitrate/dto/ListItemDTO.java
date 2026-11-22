package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.MovieListEntry;

/** Um item de lista: filme (`movie`) ou série (`series`); o outro vem nulo. */
public record ListItemDTO(String kind, MovieDTO movie, SeriesDTO series) {

    public static ListItemDTO de(MovieListEntry entrada) {
        return entrada.isSeries()
                ? new ListItemDTO("SERIES", null, new SeriesDTO(entrada.getSeries()))
                : new ListItemDTO("MOVIE", new MovieDTO(entrada.getMovie()), null);
    }
}
