package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.ReviewComment;

import java.time.Instant;

public record ReviewCommentDTO(Long id, UserSummaryDTO user, String text, Instant createdAt) {

    public ReviewCommentDTO(ReviewComment c) {
        this(c.getId(), new UserSummaryDTO(c.getUser()), c.getText(), c.getCreatedAt());
    }
}
