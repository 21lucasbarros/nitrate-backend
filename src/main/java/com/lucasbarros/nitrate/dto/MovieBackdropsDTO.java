package com.lucasbarros.nitrate.dto;

import java.util.List;

/** GET /movies/tmdb/{tmdbId}/backdrops: as cenas de um filme para escolher o fundo do perfil */
public record MovieBackdropsDTO(Long tmdbId, String title, Integer year, List<String> backdrops) {
}
