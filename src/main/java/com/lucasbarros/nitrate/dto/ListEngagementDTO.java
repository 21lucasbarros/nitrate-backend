package com.lucasbarros.nitrate.dto;

/** Curtidas e comentários da lista, e se quem vê curtiu ou salvou. `likedByMe` e `savedByMe` são null para visitantes. */
public record ListEngagementDTO(long likesCount, long commentsCount, Boolean likedByMe, Boolean savedByMe) {

    public static final ListEngagementDTO VAZIO = new ListEngagementDTO(0, 0, null, null);
}
