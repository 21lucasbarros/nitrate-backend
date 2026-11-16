package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.Notification;

import java.time.Instant;

public record NotificationDTO(Long id, Notification.Type type, String detail, Instant createdAt, boolean read,
                              MovieDTO movie) {

    public NotificationDTO(Notification n) {
        this(n.getId(), n.getType(), n.getDetail(), n.getCreatedAt(), n.getReadAt() != null, new MovieDTO(n.getMovie()));
    }
}
