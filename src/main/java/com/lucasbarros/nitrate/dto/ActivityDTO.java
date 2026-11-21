package com.lucasbarros.nitrate.dto;

import java.time.Instant;

/**
 * Um item do feed de atividade. `user` é quem fez a ação; os outros campos dependem do tipo:
 * REVIEW (review), WATCHLIST (movie), LIST (list), REVIEW_LIKE (review curtida),
 * COMMENT (review comentada + comment), FOLLOW (target, quem passou a ser seguido),
 * LIST_ENTRY (movie + list, o filme colocado numa lista; só na atividade de um filme).
 * Em REVIEW com texto, `likesCount` e `likedByMe` alimentam o botão de curtir.
 */
public record ActivityDTO(
        Type type,
        String id,
        Instant at,
        UserSummaryDTO user,
        ReviewDTO review,
        MovieDTO movie,
        MovieListDTO list,
        String comment,
        UserSummaryDTO target,
        Long likesCount,
        Boolean likedByMe) {

    public enum Type { REVIEW, WATCHLIST, LIST, REVIEW_LIKE, COMMENT, FOLLOW, LIST_ENTRY }
}
