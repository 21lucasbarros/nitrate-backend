package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Notification;
import com.lucasbarros.nitrate.entities.ReviewCollaboration;

import java.time.Instant;

// `movie` vem nas estreias de filmes e nos convites de review em conjunto, `series` em SERIES_PREMIERE
// e SERIES_SEASON (com `seasonNumber` na temporada nova), `actor` em FOLLOW e nos convites e
// `collaboration` nos convites (COLLAB_INVITE e COLLAB_ACCEPTED); os outros ficam nulos
public record NotificationDTO(Long id, Notification.Type type, String detail, Instant createdAt, boolean read,
                              MovieDTO movie, SeriesDTO series, Integer seasonNumber, UserSummaryDTO actor,
                              Collaboration collaboration) {

    // o estado do convite agora (a notificação pode ter sido vista antes de a pessoa responder)
    public record Collaboration(Long id, ReviewCollaboration.Status status, Long authorReviewId,
                                Long inviteeReviewId) {
        Collaboration(ReviewCollaboration c) {
            this(c.getId(), c.getStatus(), c.getAuthorReview().getId(),
                    c.getInviteeReview() == null ? null : c.getInviteeReview().getId());
        }
    }

    public NotificationDTO(Notification n) {
        this(n.getId(), n.getType(), n.getDetail(), n.getCreatedAt(), n.getReadAt() != null,
                filme(n) == null ? null : new MovieDTO(filme(n)),
                n.getSeries() == null ? null : new SeriesDTO(n.getSeries()),
                n.getSeasonNumber(),
                n.getActor() == null ? null : new UserSummaryDTO(n.getActor()),
                n.getCollaboration() == null ? null : new Collaboration(n.getCollaboration()));
    }

    private static Movie filme(Notification n) {
        if (n.getMovie() != null) return n.getMovie();
        return n.getCollaboration() == null ? null : n.getCollaboration().getAuthorReview().getMovie();
    }
}
