package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.RecommendationRequestDTO;
import com.lucasbarros.nitrate.dto.RecommendationResultDTO;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.graph.MovieGraph;
import com.lucasbarros.nitrate.recommender.RecommendationEngine;
import com.lucasbarros.nitrate.recommender.RecommendationResult;
import com.lucasbarros.nitrate.recommender.UserPreferences;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecommendationService {
    private static final int TOP_N_PADRAO = 5;

    @Autowired
    private MovieRepository movieRepository;

    @Transactional(readOnly = true)
    public List<RecommendationResultDTO> recomendar(RecommendationRequestDTO request) {
        List<Movie> todosOsFilmes = movieRepository.findAll();
        MovieGraph grafo = new MovieGraph(todosOsFilmes);

        UserPreferences preferencias = montarPreferencias(request, todosOsFilmes);

        RecommendationEngine engine = new RecommendationEngine(grafo);
        List<RecommendationResult> resultados = engine.recommend(preferencias, TOP_N_PADRAO);

        List<RecommendationResultDTO> dtos = new ArrayList<>();
        for (RecommendationResult resultado : resultados) {
            dtos.add(new RecommendationResultDTO(resultado));
        }
        return dtos;
    }

    private UserPreferences montarPreferencias(RecommendationRequestDTO request, List<Movie> todosOsFilmes) {
        UserPreferences preferencias = new UserPreferences();

        for (Long id : request.getLikedMovieIds()) {
            Movie filme = buscarNaLista(todosOsFilmes, id);
            preferencias.addLikedMovie(filme);
        }

        for (String diretor : request.getFavoriteDirectors()) {
            preferencias.addFavoriteDirector(diretor);
        }

        for (String ator : request.getFavoriteActors()) {
            preferencias.addFavoriteActor(ator);
        }

        return preferencias;
    }

    private Movie buscarNaLista(List<Movie> filmes, Long id) {
        for(Movie filme : filmes) {
            if(filme.getId().equals(id)) {
                return filme;
            }
        }
        throw new ResourceNotFoundException("Filme não encontrado. Id: " + id);
    }
}
