package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.WatchlistItem;

import java.time.Instant;

public class WatchlistItemDTO {
    private MovieDTO movie;
    private Instant addedAt;

    public WatchlistItemDTO(WatchlistItem entity) {
        movie = new MovieDTO(entity.getMovie());
        addedAt = entity.getAddedAt();
    }

    public MovieDTO getMovie() { return movie; }
    public Instant getAddedAt() { return addedAt; }
}
