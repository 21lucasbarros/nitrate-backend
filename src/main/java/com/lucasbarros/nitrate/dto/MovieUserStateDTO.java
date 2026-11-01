package com.lucasbarros.nitrate.dto;

// relação do usuário logado com um filme: a review dele (se houver) e se está na watchlist
public class MovieUserStateDTO {
    private ReviewDTO review;
    private boolean inWatchlist;

    public MovieUserStateDTO(ReviewDTO review, boolean inWatchlist) {
        this.review = review;
        this.inWatchlist = inWatchlist;
    }

    public ReviewDTO getReview() { return review; }
    public boolean isInWatchlist() { return inWatchlist; }
}
