package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.User;

import java.time.Instant;

public class UserProfileDTO {
    private String username;
    private String name;
    private Instant createdAt;
    private long filmsCount;
    private long reviewsCount;
    private long likesCount;
    private long watchlistCount;

    public UserProfileDTO(User user, long filmsCount, long reviewsCount, long likesCount, long watchlistCount) {
        this.username = user.getUsername();
        this.name = user.getName();
        this.createdAt = user.getCreatedAt();
        this.filmsCount = filmsCount;
        this.reviewsCount = reviewsCount;
        this.likesCount = likesCount;
        this.watchlistCount = watchlistCount;
    }

    public String getUsername() { return username; }
    public String getName() { return name; }
    public Instant getCreatedAt() { return createdAt; }
    public long getFilmsCount() { return filmsCount; }
    public long getReviewsCount() { return reviewsCount; }
    public long getLikesCount() { return likesCount; }
    public long getWatchlistCount() { return watchlistCount; }
}
