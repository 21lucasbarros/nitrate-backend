package com.lucasbarros.nitrate.dto;

/** Curtidas e comentários de uma review. `likedByMe` é null para visitantes. */
public record ReviewEngagementDTO(long likesCount, Boolean likedByMe, long commentsCount) {
}
