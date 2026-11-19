package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.User;

/** Alguém com gosto parecido: quantos filmes os dois avaliaram e o quanto as notas batem (0 a 100). */
public record SimilarUserDTO(String username, String name, String avatar, long sharedFilms, int compatibility) {

    public SimilarUserDTO(User user, long sharedFilms, int compatibility) {
        this(user.getUsername(), user.getName(), user.getAvatarPath(), sharedFilms, compatibility);
    }
}
