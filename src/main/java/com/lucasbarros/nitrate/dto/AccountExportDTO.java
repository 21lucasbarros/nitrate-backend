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
        List<FavoritePerson> favoritePeople,
        List<FilmList> lists,
        List<Comment> comments,
        List<ReviewLike> reviewLikes,
        List<String> following,
        List<String> followers,
        List<SeriesDiaryEntry> seriesDiary,
        List<SeriesWatchlistEntry> seriesWatchlist) {

    public record Show(String name, Integer firstAirYear, Long tmdbId) {}

    /** `season` nulo = a série inteira */
    public record SeriesDiaryEntry(Show series, Integer season, LocalDate watchedOn, Double rating, boolean liked,
                                   String review, boolean spoiler, Instant createdAt) {}

    public record SeriesWatchlistEntry(Show series, Instant addedAt) {}

    public record Profile(String name, String username, String email, String bio,
                          String twitter, String instagram, Instant createdAt) {}

    public record Film(String title, Integer releaseYear, Long tmdbId) {}

    public record DiaryEntry(Film film, LocalDate watchedOn, Double rating, boolean liked, boolean rewatch,
                             String review, boolean spoiler, Instant createdAt) {}

    /** ator/atriz ("actor") ou diretor(a) ("director") favorito, na ordem do perfil */
    public record FavoritePerson(String kind, String name, Long tmdbId) {}

    public record WatchlistEntry(Film film, Instant addedAt) {}

    /** comentário que a pessoa fez na review de alguém */
    public record Comment(Film film, String reviewAuthor, String text, Instant createdAt) {}

    /** review de alguém que a pessoa curtiu */
    public record ReviewLike(Film film, String reviewAuthor, Instant likedAt) {}

    public record FilmList(String name, String description, boolean isPublic, List<Film> films, Instant createdAt) {}
}
