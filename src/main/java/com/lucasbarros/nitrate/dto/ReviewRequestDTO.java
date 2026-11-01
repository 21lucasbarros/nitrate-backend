package com.lucasbarros.nitrate.dto;

import java.time.LocalDate;

public class ReviewRequestDTO {
    private Long movieId; // só usado na criação; no PUT o filme não muda
    private Double rating;
    private String text;
    private boolean liked;
    private boolean spoiler;
    private LocalDate watchedOn;

    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }
    public boolean isSpoiler() { return spoiler; }
    public void setSpoiler(boolean spoiler) { this.spoiler = spoiler; }
    public LocalDate getWatchedOn() { return watchedOn; }
    public void setWatchedOn(LocalDate watchedOn) { this.watchedOn = watchedOn; }
}
