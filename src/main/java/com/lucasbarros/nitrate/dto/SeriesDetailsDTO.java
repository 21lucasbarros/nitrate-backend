package com.lucasbarros.nitrate.dto;

import java.util.List;

/**
 * O que a página da série mostra além do que fica no banco, direto do TMDB: imagem de fundo,
 * criadores, emissoras, temporadas, próximo episódio e elenco de todas as temporadas.
 */
public record SeriesDetailsDTO(
        String backdropPath,
        String tagline,
        String status,
        boolean inProduction,
        String firstAirDate,
        String lastAirDate,
        Integer episodeRunTime,  // minutos, quando o TMDB informa
        List<Person> creators,
        List<String> networks,
        List<Season> seasons,
        NextEpisode nextEpisode,
        List<Person> cast) {

    /** `role`: personagem(ns), para o elenco; nulo para quem criou. `episodes`: em quantos episódios aparece */
    public record Person(Long tmdbId, String name, String role, String profilePath, Integer episodes) {}

    public record Season(int seasonNumber, String name, String overview, Integer episodeCount,
                         String airDate, String posterPath, Double rating) {}

    public record NextEpisode(String name, String airDate, Integer seasonNumber, Integer episodeNumber) {}
}
