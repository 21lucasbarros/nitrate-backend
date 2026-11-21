package com.lucasbarros.nitrate.dto;

/** PUT /users/me/cover: o filme do TMDB e o caminho de uma das cenas dele (de /movies/tmdb/{tmdbId}/backdrops) */
public record ProfileCoverRequestDTO(Long tmdbId, String path) {
}
