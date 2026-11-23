package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Notification;
import com.lucasbarros.nitrate.entities.ReviewCollaboration;
import com.lucasbarros.nitrate.entities.Series;
import com.lucasbarros.nitrate.entities.SeriesReview;
import com.lucasbarros.nitrate.entities.SeriesReviewCollaboration;

import java.time.Instant;

// `movie` vem nas estreias de filmes e nos convites de review em conjunto de filme, `series` em
// SERIES_PREMIERE e SERIES_SEASON (com `seasonNumber` na temporada nova) e nos convites de registro de
// série em conjunto, `actor` em FOLLOW e nos convites e `collaboration` nos convites (COLLAB_INVITE e
// COLLAB_ACCEPTED); os outros ficam nulos
public record NotificationDTO(Long id, Notification.Type type, String detail, Instant createdAt, boolean read,
                              MovieDTO movie, SeriesDTO series, Integer seasonNumber, UserSummaryDTO actor,
                              Collaboration collaboration) {

    // o estado do convite agora (a notificação pode ter sido vista antes de a pessoa responder). Em
    // `series` = convite de registro de série, com a temporada e o episódio do registro de quem convidou
    public record Collaboration(Long id, ReviewCollaboration.Status status, Long authorReviewId,
                                Long inviteeReviewId, boolean series, Integer seasonNumber, Integer episodeNumber) {
        Collaboration(ReviewCollaboration c) {
            this(c.getId(), c.getStatus(), c.getAuthorReview().getId(),
                    c.getInviteeReview() == null ? null : c.getInviteeReview().getId(), false, null, null);
        }

        Collaboration(SeriesReviewCollaboration c) {
            this(c.getId(), c.getStatus(), c.getAuthorReview().getId(),
                    c.getInviteeReview() == null ? null : c.getInviteeReview().getId(), true,
                    c.getAuthorReview().getSeasonNumber(), c.getAuthorReview().getEpisodeNumber());
        }
    }

    public NotificationDTO(Notification n) {
        this(n.getId(), n.getType(), n.getDetail(), n.getCreatedAt(), n.getReadAt() != null,
                filme(n) == null ? null : new MovieDTO(filme(n)),
                serie(n) == null ? null : new SeriesDTO(serie(n)),
                n.getSeasonNumber(),
                n.getActor() == null ? null : new UserSummaryDTO(n.getActor()),
                n.getCollaboration() != null ? new Collaboration(n.getCollaboration())
                        : n.getSeriesCollaboration() != null ? new Collaboration(n.getSeriesCollaboration()) : null);
    }

    private static Movie filme(Notification n) {
        if (n.getMovie() != null) return n.getMovie();
        return n.getCollaboration() == null ? null : n.getCollaboration().getAuthorReview().getMovie();
    }

    private static Series serie(Notification n) {
        if (n.getSeries() != null) return n.getSeries();
        SeriesReview origem = n.getSeriesCollaboration() == null ? null : n.getSeriesCollaboration().getAuthorReview();
        return origem == null ? null : origem.getSeries();
    }
}
