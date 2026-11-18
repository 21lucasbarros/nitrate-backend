package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.FavoritePerson;

/** ator/atriz ou diretor(a) favorito no perfil */
public record FavoritePersonDTO(Long tmdbId, String name, String profilePath) {
    public FavoritePersonDTO(FavoritePerson f) {
        this(f.getTmdbId(), f.getName(), f.getProfilePath());
    }
}
