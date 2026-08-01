package com.lucasbarros.nitrate.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RecommendationRequestDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<Long> likedMovieIds = new ArrayList<>();
    private List<String> favoriteDirectors = new ArrayList<>();
    private List<String> favoriteActors = new ArrayList<>();

    public List<Long> getLikedMovieIds() {
        return likedMovieIds;
    }

    public void setLikedMovieIds(List<Long> likedMovieIds) {
        this.likedMovieIds = likedMovieIds;
    }

    public List<String> getFavoriteDirectors() {
        return favoriteDirectors;
    }

    public void setFavoriteDirectors(List<String> favoriteDirectors) {
        this.favoriteDirectors = favoriteDirectors;
    }

    public List<String> getFavoriteActors() {
        return favoriteActors;
    }

    public void setFavoriteActors(List<String> favoriteActors) {
        this.favoriteActors = favoriteActors;
    }
}
