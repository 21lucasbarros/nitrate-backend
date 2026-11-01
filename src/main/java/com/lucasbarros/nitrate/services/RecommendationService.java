package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.RecommendationRequestDTO;
import com.lucasbarros.nitrate.dto.RecommendationResultDTO;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.graph.MovieGraph;
import com.lucasbarros.nitrate.graph.MovieGraphCache;
import com.lucasbarros.nitrate.recommender.RecommendationEngine;
import com.lucasbarros.nitrate.recommender.RecommendationResult;
import com.lucasbarros.nitrate.recommender.UserPreferences;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class RecommendationService {
    private static final int TOP_N_PADRAO = 5;
    private static final int TOP_N_USUARIO = 12;
    private static final double NOTA_MINIMA_SEMENTE = 3.5;

    @Autowired
    private MovieGraphCache graphCache;

    @Autowired
    private ReviewRepository reviewRepository;

    /**
     * Recomendações a partir do diário: os filmes que a pessoa curtiu ou avaliou bem são as
     * sementes do grafo. Se não houver nenhum, usa os registrados sem nota. Tudo que ela já
     * registrou fica de fora do resultado.
     */
    public List<RecommendationResultDTO> recomendarParaUsuario(User user) {
        MovieGraph grafo = graphCache.obter();
        Map<Long, Movie> filmesPorId = new HashMap<>();
        for (Movie filme : grafo.getAllMovies()) {
            filmesPorId.put(filme.getId(), filme);
        }

        List<Review> reviews = reviewRepository.findByUserOrderByCreatedAtDesc(user);
        Set<Long> jaRegistrados = new HashSet<>();
        List<Long> sementes = new ArrayList<>();
        List<Long> semNota = new ArrayList<>();
        for (Review review : reviews) {
            Long id = review.getMovie().getId();
            jaRegistrados.add(id);
            if (review.isLiked() || (review.getRating() != null && review.getRating() >= NOTA_MINIMA_SEMENTE)) {
                sementes.add(id);
            } else if (review.getRating() == null) {
                semNota.add(id);
            }
        }
        if (sementes.isEmpty()) {
            sementes = semNota;
        }

        UserPreferences preferencias = new UserPreferences();
        for (Long id : sementes) {
            Movie filme = filmesPorId.get(id);
            if (filme != null) {
                preferencias.addLikedMovie(filme);
            }
        }
        if (preferencias.getLikedMovies().isEmpty()) {
            return new ArrayList<>();
        }

        // pede mais que o necessário porque os já registrados (mas não curtidos) são descartados
        List<RecommendationResult> resultados = new RecommendationEngine(grafo)
                .recommend(preferencias, TOP_N_USUARIO + jaRegistrados.size());

        List<RecommendationResultDTO> dtos = new ArrayList<>();
        for (RecommendationResult resultado : resultados) {
            if (jaRegistrados.contains(resultado.getMovie().getId())) continue;
            dtos.add(new RecommendationResultDTO(resultado));
            if (dtos.size() == TOP_N_USUARIO) break;
        }
        return dtos;
    }

    public List<RecommendationResultDTO> recomendar(RecommendationRequestDTO request) {
        // o grafo vem do cache; os filmes curtidos precisam ser as mesmas instâncias que estão nele
        MovieGraph grafo = graphCache.obter();
        List<Movie> todosOsFilmes = grafo.getAllMovies();

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
