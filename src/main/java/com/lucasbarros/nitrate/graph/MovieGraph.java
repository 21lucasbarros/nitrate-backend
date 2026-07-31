package com.lucasbarros.nitrate.graph;

import com.lucasbarros.nitrate.entities.Genre;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.MovieCast;

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

            for(MovieCast membro : movie.getCast()) {
                adicionarNoIndice(byActor, membro.getActor().getName().toLowerCase(), movie);
            }

            for(Genre genero : movie.getGenres()) {
                adicionarNoIndice(byGenre, genero.getName().toLowerCase(), movie);
            }

            if(movie.getFranchise() != null) {
                adicionarNoIndice(byFranchise, movie.getFranchise().getName().toLowerCase(), movie);
            }

            adicionarNoIndiceEra(movie.getEra(), movie);
        }
    }

    public List<Edge> getConnections(Movie source) {
        List<Edge> edges = new ArrayList<>();
        edges.addAll(conectarPorDiretor(source));
        edges.addAll(conectarPorAtores(source));
        edges.addAll(conectarPorGeneros(source));
        edges.addAll(conectarPorFranquia(source));
        edges.addAll(conectarPorEpoca(source));
        return edges;
    }

    private List<Edge> conectarPorDiretor(Movie source) {
        List<Edge> edges = new ArrayList<>();
        if(source.getDirector() == null) {
            return edges;
        }

        String nomeDoDiretor = source.getDirector().getName();
        List<Movie> candidatos = byDirection.get(nomeDoDiretor.toLowerCase());
        if(candidatos == null) {
            return edges;
        }

        for(Movie candidato : candidatos) {
            if(candidato.equals(source)) continue;
            String reason = "mesmo diretor (" + nomeDoDiretor + ")";
            edges.add(new Edge(candidato, ConnectionType.DIRECTOR, ConnectionType.DIRECTOR.getBaseWeight(), reason, nomeDoDiretor));
        }
        return edges;
    }

    private List<Edge> conectarPorAtores(Movie source) {
        List<Edge> edges = new ArrayList<>();

        for(MovieCast membro : source.getCast()) {
            String nomeDoAtor = membro.getActor().getName();
            List<Movie> candidatos = byActor.get(nomeDoAtor.toLowerCase());
            if(candidatos == null) continue;

            for(Movie candidato : candidatos) {
                if(candidato.equals(source)) continue;

                MovieCast outroMembro = candidato.findCastMember(nomeDoAtor);
                if(outroMembro == null) continue;

                double roleFactor = Math.min(membro.getRole().getRelevance(), outroMembro.getRole().getRelevance());
                double weight = ConnectionType.ACTOR.getBaseWeight() * roleFactor;

                String reason = "elenco em comum: " + nomeDoAtor;
                edges.add(new Edge(candidato, ConnectionType.ACTOR, weight, reason, nomeDoAtor));
            }
        }
        return edges;
    }

    private List<Edge> conectarPorGeneros(Movie source) {
        List<Edge> edges = new ArrayList<>();

        for (Genre genero : source.getGenres()) {
            List<Movie> candidatos = byGenre.get(genero.getName().toLowerCase());
            if (candidatos == null) continue;

            for (Movie candidato : candidatos) {
                if (candidato.equals(source)) continue;
                String reason = "mesmo gênero: " + genero.getName();
                edges.add(new Edge(candidato, ConnectionType.GENRE, ConnectionType.GENRE.getBaseWeight(), reason, genero.getName()));
            }
        }
        return edges;
    }

    private List<Edge> conectarPorFranquia(Movie source) {
        List<Edge> edges = new ArrayList<>();
        if (source.getFranchise() == null) {
            return edges;
        }

        String nomeDaFranquia = source.getFranchise().getName();
        List<Movie> candidatos = byFranchise.get(nomeDaFranquia.toLowerCase());
        if (candidatos == null) {
            return edges;
        }

        for (Movie candidato : candidatos) {
            if (candidato.equals(source)) continue;
            String reason = "mesma franquia: " + nomeDaFranquia;
            edges.add(new Edge(candidato, ConnectionType.FRANCHISE, ConnectionType.FRANCHISE.getBaseWeight(), reason, nomeDaFranquia));
        }
        return edges;
    }

    private List<Edge> conectarPorEpoca(Movie source) {
        List<Edge> edges = new ArrayList<>();
        List<Movie> candidatos = byEra.get(source.getEra());
        if (candidatos == null) {
            return edges;
        }

        for (Movie candidato : candidatos) {
            if (candidato.equals(source)) continue;
            String reason = "mesma época: década de " + source.getEra();
            edges.add(new Edge(candidato, ConnectionType.ERA, ConnectionType.ERA.getBaseWeight(), reason, String.valueOf(source.getEra())));
        }
        return edges;
    }

    public List<Movie> getAllMovies() {
        return movies;
    }
}
