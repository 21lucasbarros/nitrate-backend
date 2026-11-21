package com.lucasbarros.nitrate.dto;

import java.util.List;

/** Uma temporada com os episódios, direto do TMDB. */
public record SeasonDTO(int seasonNumber, String name, String overview, String airDate, String posterPath,
                        List<Episode> episodes) {

    public record Episode(int episodeNumber, String name, String overview, String airDate, Integer runtime,
                          String stillPath, Double rating) {}
}
