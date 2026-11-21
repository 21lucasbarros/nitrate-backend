package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.UserAchievement;

import java.time.Instant;

/** conquista no perfil; `hidden` só aparece como true para a dona (visitantes nem recebem as ocultas) */
public record AchievementDTO(Long id, String type, Person person, int films, boolean hidden, Instant achievedAt) {

    /** `slug` nulo: o link usa o id */
    public record Person(Long tmdbId, String name, String profilePath, String slug) {}

    public AchievementDTO(UserAchievement a, String slug) {
        this(a.getId(), a.getType(), new Person(a.getPersonTmdbId(), a.getPersonName(), a.getPersonProfilePath(), slug),
                a.getFilms(), a.isHidden(), a.getAchievedAt());
    }
}
