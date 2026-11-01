package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.Review;

import java.time.Instant;
import java.time.LocalDate;

public class ReviewDTO {
    private Long id;
    private MovieDTO movie;
    private UserSummaryDTO user;
    private Double rating;
    private String text;
    private boolean liked;
    private boolean spoiler;
    private LocalDate watchedOn;
    private Instant createdAt;

    public ReviewDTO(Review entity) {
        id = entity.getId();
        movie = new MovieDTO(entity.getMovie());
        user = new UserSummaryDTO(entity.getUser());
        rating = entity.getRating();
        text = entity.getText();
        liked = entity.isLiked();
        spoiler = entity.isSpoiler();
        watchedOn = entity.getWatchedOn();
        createdAt = entity.getCreatedAt();
    }

    public Long getId() { return id; }
    public MovieDTO getMovie() { return movie; }
    public UserSummaryDTO getUser() { return user; }
    public Double getRating() { return rating; }
    public String getText() { return text; }
    public boolean isLiked() { return liked; }
    public boolean isSpoiler() { return spoiler; }
    public LocalDate getWatchedOn() { return watchedOn; }
    public Instant getCreatedAt() { return createdAt; }
}
