package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.MovieDTO;
import com.lucasbarros.nitrate.dto.TasteCandidateGroupDTO;
import com.lucasbarros.nitrate.dto.TasteProfileDTO;
import com.lucasbarros.nitrate.dto.TasteQuizDTO;
import com.lucasbarros.nitrate.dto.TasteRequestDTO;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.TasteProfile;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.graph.MovieGraphCache;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.repositories.TasteProfileRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.taste.TasteMatcher;
import com.lucasbarros.nitrate.taste.TasteMatcher.Preferencias;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** questionário de gostos do onboarding: as perguntas, a grade de filmes e as respostas salvas */
@Service
public class TasteService {

    private static final int CANDIDATOS = 30;
    private static final int MAX_FILMES = 30;

    @Autowired private TasteProfileRepository tasteProfileRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private MovieGraphCache graphCache;
    @Autowired private TasteMatcher tasteMatcher;
    @Autowired private RecommendationService recommendationService;

    public TasteQuizDTO quiz() {
        return TasteQuizDTO.doQuestionario();
    }

    /** filmes do acervo para a pessoa marcar os que já viu e gostou, conforme as respostas */
    @Transactional(readOnly = true)
    public List<TasteCandidateGroupDTO> candidatos(User user, TasteRequestDTO request) {
        Preferencias prefs = Preferencias.de(request.genres(), request.answers(), request.eras());
        // o que ela já registrou no diário não precisa ser perguntado de novo
        Set<Long> registrados = reviewRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(Review::getMovie).map(Movie::getId).collect(Collectors.toSet());
        // filmes do grafo: já vêm com gêneros, diretor e franquia carregados
        return tasteMatcher.grupos(prefs, graphCache.obter().getAllMovies(), registrados, CANDIDATOS).stream()
                .map(g -> new TasteCandidateGroupDTO(g.id(), g.label(), g.filmes().stream().map(MovieDTO::new).toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public TasteProfileDTO perfil(User user) {
        return tasteProfileRepository.findById(user.getId()).map(this::paraDTO).orElseGet(TasteProfileDTO::vazio);
    }

    /** substitui as respostas inteiras; pular tudo também salva (vazio), para não perguntar de novo */
    @Transactional
    public TasteProfileDTO salvar(User user, TasteRequestDTO request) {
        Preferencias prefs = Preferencias.de(request.genres(), request.answers(), request.eras());
        TasteProfile perfil = tasteProfileRepository.findById(user.getId())
                .orElseGet(() -> new TasteProfile(userRepository.getReferenceById(user.getId())));

        perfil.getGenres().clear();
        perfil.getGenres().addAll(prefs.genres());
        perfil.getAnswers().clear();
        perfil.getAnswers().addAll(prefs.answers());
        perfil.getEras().clear();
        perfil.getEras().addAll(prefs.eras());
        perfil.getSeedMovieIds().clear();
        perfil.getSeedMovieIds().addAll(filmesValidos(request.movieIds()));
        perfil.setUpdatedAt(Instant.now());

        TasteProfileDTO dto = paraDTO(tasteProfileRepository.save(perfil));
        recommendationService.invalidar(user.getId());
        return dto;
    }

    /** sem repetir, só filmes que existem, na ordem em que foram marcados */
    private List<Long> filmesValidos(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        List<Long> unicos = new ArrayList<>(new LinkedHashSet<>(ids.stream().filter(Objects::nonNull).toList()));
        if (unicos.size() > MAX_FILMES) unicos = unicos.subList(0, MAX_FILMES);
        Set<Long> existentes = movieRepository.findAllById(unicos).stream().map(Movie::getId).collect(Collectors.toSet());
        return unicos.stream().filter(existentes::contains).toList();
    }

    private TasteProfileDTO paraDTO(TasteProfile perfil) {
        Map<Long, Movie> porId = movieRepository.findAllById(perfil.getSeedMovieIds()).stream()
                .collect(Collectors.toMap(Movie::getId, Function.identity()));
        List<MovieDTO> filmes = perfil.getSeedMovieIds().stream()
                .map(porId::get).filter(Objects::nonNull).map(MovieDTO::new).toList();
        return new TasteProfileDTO(List.copyOf(perfil.getGenres()), List.copyOf(perfil.getAnswers()),
                List.copyOf(perfil.getEras()), filmes, perfil.getUpdatedAt());
    }
}
