package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.RecommendationRequestDTO;
import com.lucasbarros.nitrate.dto.RecommendationResultDTO;
import com.lucasbarros.nitrate.entities.FavoritePerson;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.graph.MovieGraph;
import com.lucasbarros.nitrate.graph.MovieGraphCache;
import com.lucasbarros.nitrate.recommender.RecommendationEngine;
import com.lucasbarros.nitrate.recommender.RecommendationResult;
import com.lucasbarros.nitrate.recommender.UserPreferences;
import com.lucasbarros.nitrate.repositories.FavoritePersonRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RecommendationService {
    private static final int TOP_N_PADRAO = 5;
    private static final int TOP_N_USUARIO = 12;
    private static final double NOTA_MINIMA_SEMENTE = 3.5;
    private static final int MAX_SEMENTES = 5;
    private static final int MAX_POR_DIRETOR = 2;
    private static final int MAX_POR_FRANQUIA = 2;
    private static final Duration VALIDADE_CACHE = Duration.ofMinutes(10);

    private record RecomendacoesEmCache(List<RecommendationResultDTO> resultado, Instant em) {}

    private final Map<Long, RecomendacoesEmCache> cache = new ConcurrentHashMap<>();

    @Autowired
    private MovieGraphCache graphCache;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private FavoritePersonRepository favoritePersonRepository;

    /**
     * Recomendações a partir do diário: os últimos filmes que a pessoa curtiu ou avaliou bem
     * são as sementes do grafo. Se não houver nenhum, usa os últimos registrados sem nota. Tudo
     * que ela já registrou (inclusive o histórico antigo) fica de fora do resultado.
     */
    public List<RecommendationResultDTO> recomendarParaUsuario(User user) {
        // a home pede isso em toda visita: guardamos o resultado por alguns minutos,
        // e qualquer mudança no diário da pessoa descarta o que estava guardado
        RecomendacoesEmCache emCache = cache.get(user.getId());
        if (emCache != null && emCache.em().plus(VALIDADE_CACHE).isAfter(Instant.now())) {
            return emCache.resultado();
        }
        List<RecommendationResultDTO> resultado = List.copyOf(calcularParaUsuario(user));
        cache.put(user.getId(), new RecomendacoesEmCache(resultado, Instant.now()));
        return resultado;
    }

    /** o diário ou os favoritos da pessoa mudaram: a próxima visita recalcula as recomendações */
    public void invalidar(Long userId) {
        cache.remove(userId);
    }

    private List<RecommendationResultDTO> calcularParaUsuario(User user) {
        MovieGraph grafo = graphCache.obter();
        Map<Long, Movie> filmesPorId = new HashMap<>();
        for (Movie filme : grafo.getAllMovies()) {
            filmesPorId.put(filme.getId(), filme);
        }

        // do mais recente para o mais antigo pela data em que assistiu (o import do Letterboxd
        // cria tudo no mesmo instante, então a data de registro não diz nada); sem data, vai pro fim
        List<Review> reviews = new ArrayList<>(reviewRepository.findByUserOrderByCreatedAtDesc(user));
        reviews.sort(Comparator.comparing(Review::getWatchedOn, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Review::getCreatedAt, Comparator.reverseOrder()));

        Set<Long> jaRegistrados = new HashSet<>();
        List<Long> sementes = new ArrayList<>();
        List<Long> semNota = new ArrayList<>();
        for (Review review : reviews) {
            Long id = review.getMovie().getId();
            jaRegistrados.add(id);
            // só os últimos filmes de que a pessoa gostou guiam o grafo: o gosto do momento,
            // não o histórico inteiro (que deixava tudo "reforçado por 86 filmes curtidos")
            if (review.isLiked() || (review.getRating() != null && review.getRating() >= NOTA_MINIMA_SEMENTE)) {
                if (sementes.size() < MAX_SEMENTES && !sementes.contains(id)) sementes.add(id);
            } else if (review.getRating() == null) {
                if (semNota.size() < MAX_SEMENTES && !semNota.contains(id)) semNota.add(id);
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
        // atores e diretores favoritos do perfil: filmes ligados a eles ganham peso extra
        for (FavoritePerson favorito : favoritePersonRepository.findByUserOrderByPositionAsc(user)) {
            if (favorito.getKind() == FavoritePerson.Kind.ACTOR) {
                preferencias.addFavoriteActor(favorito.getName());
            } else {
                preferencias.addFavoriteDirector(favorito.getName());
            }
        }

        // pede mais que o necessário porque os já registrados (mas não curtidos) e os que
        // estourarem o limite por diretor/franquia são descartados
        List<RecommendationResult> resultados = new RecommendationEngine(grafo)
                .recommend(preferencias, TOP_N_USUARIO * 4 + jaRegistrados.size());

        // variedade: sem limite, um diretor ou uma saga que a pessoa ama ocupa a home inteira
        Map<String, Integer> porDiretor = new HashMap<>();
        Map<String, Integer> porFranquia = new HashMap<>();
        List<RecommendationResultDTO> dtos = new ArrayList<>();
        for (RecommendationResult resultado : resultados) {
            Movie filme = resultado.getMovie();
            if (jaRegistrados.contains(filme.getId())) continue;

            String diretor = filme.getDirector() != null ? filme.getDirector().getName().toLowerCase() : null;
            String franquia = filme.getFranchise() != null ? filme.getFranchise().getName().toLowerCase() : null;
            if (diretor != null && porDiretor.getOrDefault(diretor, 0) >= MAX_POR_DIRETOR) continue;
            if (franquia != null && porFranquia.getOrDefault(franquia, 0) >= MAX_POR_FRANQUIA) continue;
            if (diretor != null) porDiretor.merge(diretor, 1, Integer::sum);
            if (franquia != null) porFranquia.merge(franquia, 1, Integer::sum);

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
