package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.ListComment;

import java.time.Instant;

public record ListCommentDTO(Long id, UserSummaryDTO user, String text, Instant createdAt) {

    public ListCommentDTO(ListComment c) {
        this(c.getId(), new UserSummaryDTO(c.getUser()), c.getText(), c.getCreatedAt());
    }
}
