package com.lucasbarros.nitrate.dto;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/**
 * Cópia de tudo o que o Nitrate guarda sobre a pessoa (LGPD, art. 18: acesso e portabilidade).
 * Tem o e-mail, então só sai em /users/me/export, para a própria pessoa.
 */
public record AccountExportDTO(
        Instant exportedAt,
        Profile profile,
        List<DiaryEntry> diary,
        List<WatchlistEntry> watchlist,
        List<Film> favorites,
        List<String> following,
        List<String> followers) {

    public record Profile(String name, String username, String email, String bio,
                          String twitter, String instagram, Instant createdAt) {}

    public record Film(String title, Integer releaseYear, Long tmdbId) {}

    public record DiaryEntry(Film film, LocalDate watchedOn, Double rating, boolean liked, boolean rewatch,
                             String review, boolean spoiler, Instant createdAt) {}

    public record WatchlistEntry(Film film, Instant addedAt) {}
}
