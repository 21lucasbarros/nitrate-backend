package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.WatchlistItem;

import java.time.Instant;

/** Um filme que alguém que você segue guardou na watchlist. */
public record FriendWatchlistItemDTO(UserSummaryDTO user, MovieDTO movie, Instant addedAt) {

    public FriendWatchlistItemDTO(WatchlistItem item) {
        this(new UserSummaryDTO(item.getUser()), new MovieDTO(item.getMovie()), item.getAddedAt());
    }
}
