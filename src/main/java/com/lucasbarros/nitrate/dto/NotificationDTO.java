package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.Notification;

import java.time.Instant;

// `movie` vem nas estreias e `actor` em FOLLOW; o outro fica nulo
public record NotificationDTO(Long id, Notification.Type type, String detail, Instant createdAt, boolean read,
                              MovieDTO movie, UserSummaryDTO actor) {

    public NotificationDTO(Notification n) {
        this(n.getId(), n.getType(), n.getDetail(), n.getCreatedAt(), n.getReadAt() != null,
                n.getMovie() == null ? null : new MovieDTO(n.getMovie()),
                n.getActor() == null ? null : new UserSummaryDTO(n.getActor()));
    }
}
