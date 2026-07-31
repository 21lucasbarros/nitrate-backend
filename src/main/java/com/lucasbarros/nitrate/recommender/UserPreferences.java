package com.lucasbarros.nitrate.recommender;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.graph.ConnectionType;

import java.util.*;

public class UserPreferences {
    private List<Movie> likedMovies = new ArrayList<>();
    private Set<String> favoriteDirectors = new HashSet<>();
    private Set<String> favoriteActors = new HashSet<>();
    private Map<ConnectionType, Double> connectionTypeMultipliers = new HashMap<>();

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

    public void addFavoriteDirector(String director) {
        favoriteActors.add(director.toLowerCase());
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
}
