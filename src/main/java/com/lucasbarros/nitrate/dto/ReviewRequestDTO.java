package com.lucasbarros.nitrate.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.util.List;

public class ReviewRequestDTO {
    private Long movieId; // só usado na criação; no PUT o filme não muda
    private Double rating;
    private String text;
    private boolean liked;
    private boolean spoiler;
    private boolean rewatch;
    private LocalDate watchedOn;
    // review em conjunto: os usernames de todo mundo que assistiu junto, até 15. nulo = não muda
    // (na criação, ninguém); quem sai da lista com o convite ainda não aceito tem o convite desfeito.
    // Um texto só também vale (o formato antigo, de uma pessoa; vazio = ninguém)
    @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
    private List<String> watchedWith;

    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }
    public boolean isRewatch() { return rewatch; }
    public void setRewatch(boolean rewatch) { this.rewatch = rewatch; }
    public boolean isSpoiler() { return spoiler; }
    public void setSpoiler(boolean spoiler) { this.spoiler = spoiler; }
    public LocalDate getWatchedOn() { return watchedOn; }
    public void setWatchedOn(LocalDate watchedOn) { this.watchedOn = watchedOn; }
    public List<String> getWatchedWith() { return watchedWith; }
    public void setWatchedWith(List<String> watchedWith) { this.watchedWith = watchedWith; }
}
