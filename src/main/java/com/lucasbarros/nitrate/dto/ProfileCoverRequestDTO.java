package com.lucasbarros.nitrate.dto;

/**
 * PUT /users/me/cover: o filme do TMDB, o caminho de uma das cenas dele (de /movies/tmdb/{tmdbId}/backdrops)
 * e, opcionalmente, o enquadramento (x e y de 0 a 100, como o object-position do CSS)
 */
public record ProfileCoverRequestDTO(Long tmdbId, String path, Integer x, Integer y) {
}
