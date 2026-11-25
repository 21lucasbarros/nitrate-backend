package com.lucasbarros.nitrate.recommender;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.graph.ConnectionType;

import java.util.*;

public class UserPreferences {
    private List<Movie> likedMovies = new ArrayList<>();
    // quanto cada semente pesa (nota e quão recente); sem peso, vale 1
    private Map<Movie, Double> seedWeights = new HashMap<>();
    // nota baixa ou "não tenho interesse": o que se parece com eles perde pontos
    private List<Movie> dislikedMovies = new ArrayList<>();
    private Set<String> favoriteDirectors = new HashSet<>();
    private Set<String> favoriteActors = new HashSet<>();
    private Map<ConnectionType, Double> connectionTypeMultipliers = new HashMap<>();
    // na medição, as curtidas da própria pessoa não entram no "quem curtiu também curtiu"
    private Long ignoredUserId;

    private static final double FAVORITE_BOOST = 1.6;

    public UserPreferences() {
        for(ConnectionType type : ConnectionType.values()) {
            connectionTypeMultipliers.put(type, 1.0);
        }
    }

    public void addLikedMovie(Movie movie) {
        if (!likedMovies.contains(movie)) {
            likedMovies.add(movie);
        }
    }

    public void addLikedMovie(Movie movie, double weight) {
        addLikedMovie(movie);
        seedWeights.put(movie, weight);
    }

    public void addDislikedMovie(Movie movie) {
        if (!dislikedMovies.contains(movie)) {
            dislikedMovies.add(movie);
        }
    }

    public void addFavoriteDirector(String director) {
        favoriteDirectors.add(director.toLowerCase());
    }

    public void addFavoriteActor(String actor) {
        favoriteActors.add(actor.toLowerCase());
    }

    public void setConnectionTypeMultipliers(ConnectionType type, double multiplier) {
        connectionTypeMultipliers.put(type, multiplier);
    }

    public List<Movie> getLikedMovies() {
        return likedMovies;
    }

    public double getSeedWeight(Movie movie) {
        return seedWeights.getOrDefault(movie, 1.0);
    }

    public List<Movie> getDislikedMovies() {
        return dislikedMovies;
    }

    public boolean isFavoriteDirector(String director) {
        if(director == null) return false;
        return favoriteDirectors.contains(director.toLowerCase());
    }

    public boolean isFavoriteActor(String actor) {
        if (actor == null) return false;
        return favoriteActors.contains(actor.toLowerCase());
    }

    public double getConnectionTypeMultiplier(ConnectionType type) {
        if (connectionTypeMultipliers.containsKey(type)) {
            return connectionTypeMultipliers.get(type);
        }
        return 1.0;
    }

    public double getFavoriteBoost() {
        return FAVORITE_BOOST;
    }

    public Long getIgnoredUserId() {
        return ignoredUserId;
    }

    public void setIgnoredUserId(Long ignoredUserId) {
        this.ignoredUserId = ignoredUserId;
    }
}
