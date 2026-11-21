package com.lucasbarros.nitrate.dto;

import java.time.Instant;
import java.util.List;

/** relação do usuário logado com uma série: os registros (mais recente primeiro) e a watchlist */
public record SeriesUserStateDTO(List<SeriesReviewDTO> reviews, boolean inWatchlist) {

    public record WatchlistItem(SeriesDTO series, Instant addedAt) {}
}
