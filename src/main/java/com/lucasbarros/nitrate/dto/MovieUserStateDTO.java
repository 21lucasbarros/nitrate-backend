package com.lucasbarros.nitrate.dto;

// relação do usuário logado com um filme: a review mais recente, quantas vezes viu e a watchlist
public class MovieUserStateDTO {
    private ReviewDTO review;
    private long reviewsCount;
    private boolean inWatchlist;

    public MovieUserStateDTO(ReviewDTO review, long reviewsCount, boolean inWatchlist) {
        this.review = review;
        this.reviewsCount = reviewsCount;
        this.inWatchlist = inWatchlist;
    }

    public ReviewDTO getReview() { return review; }
    public long getReviewsCount() { return reviewsCount; }
    public boolean isInWatchlist() { return inWatchlist; }
}
