package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.Notification;

import java.time.Instant;

// `movie` vem nas estreias de filmes, `series` em SERIES_PREMIERE e SERIES_SEASON (com `seasonNumber`
// na temporada nova) e `actor` em FOLLOW; os outros ficam nulos
public record NotificationDTO(Long id, Notification.Type type, String detail, Instant createdAt, boolean read,
                              MovieDTO movie, SeriesDTO series, Integer seasonNumber, UserSummaryDTO actor) {

    public NotificationDTO(Notification n) {
        this(n.getId(), n.getType(), n.getDetail(), n.getCreatedAt(), n.getReadAt() != null,
                n.getMovie() == null ? null : new MovieDTO(n.getMovie()),
                n.getSeries() == null ? null : new SeriesDTO(n.getSeries()),
                n.getSeasonNumber(),
                n.getActor() == null ? null : new UserSummaryDTO(n.getActor()));
    }
}
