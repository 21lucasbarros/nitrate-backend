package com.lucasbarros.nitrate.graph;

import com.lucasbarros.nitrate.entities.Movie;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MovieGraph {
    private List<Movie> movies;

    private Map<String, List<Movie>> byDirection = new HashMap<>();
    private Map<String, List<Movie>> byActor = new HashMap<>();
    private Map<String, List<Movie>> byGenre = new HashMap<>();
    private Map<String, List<Movie>> byFranchise = new HashMap<>();
    private Map<Integer, List<Movie>> byEra = new HashMap<>();

    public MovieGraph(List<Movie> movies) {
        this.movies = movies;
    }

    private void adicionarNoIndice(Map<String, List<Movie>> mapa, String chave, Movie movie) {
        List<Movie> lista = mapa.get(chave);
        if(lista == null) {
            lista = new ArrayList<>();
            mapa.put(chave, lista);
        }
        lista.add(movie);
    }

    private void adicionarNoIndiceEra(int era, Movie movie) {
        List<Movie> lista = byEra.get(era);
        if(lista == null) {
            lista = new ArrayList<>();
            byEra.put(era, lista);
        }
        lista.add(movie);
    }

    private void montarIndice() {
        for(Movie movie : movies) {
            if(movie.getDirector() != null) {
                adicionarNoIndice(byDirection, movie.getDirector().getName().toLowerCase(), movie);
            }
        }
    }
}
