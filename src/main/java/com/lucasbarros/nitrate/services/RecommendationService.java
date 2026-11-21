package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.RecommendationRequestDTO;
import com.lucasbarros.nitrate.dto.RecommendationResultDTO;
import com.lucasbarros.nitrate.entities.FavoritePerson;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.TasteProfile;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.graph.MovieGraph;
import com.lucasbarros.nitrate.graph.MovieGraphCache;
import com.lucasbarros.nitrate.recommender.RecommendationEngine;
import com.lucasbarros.nitrate.recommender.RecommendationResult;
import com.lucasbarros.nitrate.recommender.UserPreferences;
import com.lucasbarros.nitrate.repositories.FavoritePersonRepository;
import com.lucasbarros.nitrate.repositories.TasteProfileRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import com.lucasbarros.nitrate.taste.TasteMatcher;
import com.lucasbarros.nitrate.taste.TasteMatcher.Preferencias;
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
    // empurrão para os filmes que combinam com as respostas do questionário de gostos
    private static final double BONUS_GOSTO = 1.25;

    private record RecomendacoesEmCache(List<RecommendationResultDTO> resultado, Instant em) {}

    private final Map<Long, RecomendacoesEmCache> cache = new ConcurrentHashMap<>();

    @Autowired
    private MovieGraphCache graphCache;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private FavoritePersonRepository favoritePersonRepository;

    @Autowired
    private TasteProfileRepository tasteProfileRepository;

    @Autowired
    private TasteMatcher tasteMatcher;

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
        // quantas curtidas de verdade guiam o grafo: com poucas, as respostas do questionário mandam mais
        int curtidasReais = sementes.size();

        // questionário do onboarding: os filmes que a pessoa marcou completam as vagas de semente.
        // Conforme ela curte filmes de verdade, eles perdem espaço até sumirem
        TasteProfile gosto = tasteProfileRepository.findById(user.getId()).orElse(null);
        Preferencias prefs = gosto == null ? null
                : Preferencias.de(gosto.getGenres(), gosto.getAnswers(), gosto.getEras());
        boolean temGosto = prefs != null && !prefs.vazia();
        if (gosto != null) {
            // disse que já viu: não faz sentido recomendar
            jaRegistrados.addAll(gosto.getSeedMovieIds());
            for (Long id : gosto.getSeedMovieIds()) {
                if (sementes.size() >= MAX_SEMENTES) break;
                if (!sementes.contains(id)) sementes.add(id);
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
            // nenhum filme para guiar o grafo: se respondeu o questionário, vão os que mais combinam
            if (!temGosto) return new ArrayList<>();
            return porGosto(prefs, grafo, jaRegistrados, TOP_N_USUARIO).stream().map(RecommendationResultDTO::new).toList();
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
        if (temGosto) {
            resultados = comEmpurrao(resultados, prefs, curtidasReais);
        }

        List<RecommendationResult> escolhidos = variados(resultados, jaRegistrados);
        if (temGosto && curtidasReais < MAX_SEMENTES) {
            escolhidos = garantirGosto(escolhidos, prefs, grafo, jaRegistrados);
        }
        return escolhidos.stream().map(RecommendationResultDTO::new).toList();
    }

    /**
     * Os TOP_N_USUARIO primeiros, sem os já registrados e com variedade: sem limite, um diretor ou
     * uma saga que a pessoa ama ocupa a home inteira.
     */
    private List<RecommendationResult> variados(List<RecommendationResult> resultados, Set<Long> jaRegistrados) {
        Map<String, Integer> porDiretor = new HashMap<>();
        Map<String, Integer> porFranquia = new HashMap<>();
        Set<Long> vistos = new HashSet<>();
        List<RecommendationResult> escolhidos = new ArrayList<>();
        for (RecommendationResult resultado : resultados) {
            Movie filme = resultado.getMovie();
            if (jaRegistrados.contains(filme.getId()) || !vistos.add(filme.getId())) continue;

            String diretor = filme.getDirector() != null ? filme.getDirector().getName().toLowerCase() : null;
            String franquia = filme.getFranchise() != null ? filme.getFranchise().getName().toLowerCase() : null;
            if (diretor != null && porDiretor.getOrDefault(diretor, 0) >= MAX_POR_DIRETOR) continue;
            if (franquia != null && porFranquia.getOrDefault(franquia, 0) >= MAX_POR_FRANQUIA) continue;
            if (diretor != null) porDiretor.merge(diretor, 1, Integer::sum);
            if (franquia != null) porFranquia.merge(franquia, 1, Integer::sum);

            escolhidos.add(resultado);
            if (escolhidos.size() == TOP_N_USUARIO) break;
        }
        return escolhidos;
    }

    /**
     * O que combina com as respostas sobe, com o motivo junto. Quanto menos curtidas de verdade,
     * mais forte: quem acabou de responder "comédia romântica" e marcou dois filmes não pode receber
     * a filmografia inteira do diretor de um deles.
     */
    private List<RecommendationResult> comEmpurrao(List<RecommendationResult> resultados, Preferencias prefs,
                                                   int curtidasReais) {
        double combina = curtidasReais == 0 ? 2.0 : curtidasReais < MAX_SEMENTES ? 1.5 : BONUS_GOSTO;
        double naoCombina = curtidasReais == 0 ? 0.4 : curtidasReais < MAX_SEMENTES ? 0.75 : 1.0;
        List<RecommendationResult> ajustados = new ArrayList<>(resultados.size());
        for (RecommendationResult r : resultados) {
            List<String> motivos = tasteMatcher.motivos(prefs, r.getMovie());
            if (motivos.isEmpty()) {
                ajustados.add(new RecommendationResult(r.getMovie(), r.getScore() * naoCombina, r.getReasons()));
            } else {
                // o motivo do questionário vem primeiro: é o que a pessoa reconhece como dela
                List<String> razoes = new ArrayList<>();
                razoes.add("combina com o que você curte: " + motivos.get(0));
                razoes.addAll(r.getReasons());
                ajustados.add(new RecommendationResult(r.getMovie(), r.getScore() * combina, razoes));
            }
        }
        ajustados.sort(Comparator.comparingDouble(RecommendationResult::getScore).reversed());
        return ajustados;
    }

    /**
     * Enquanto o diário é pequeno, pelo menos metade das recomendações combina com as respostas:
     * o que o grafo trouxe e combina vem primeiro, depois os melhores do questionário, e o resto do
     * grafo completa.
     */
    private List<RecommendationResult> garantirGosto(List<RecommendationResult> escolhidos, Preferencias prefs,
                                                     MovieGraph grafo, Set<Long> jaRegistrados) {
        int minimo = TOP_N_USUARIO / 2;
        List<RecommendationResult> combinam = new ArrayList<>();
        List<RecommendationResult> resto = new ArrayList<>();
        for (RecommendationResult r : escolhidos) {
            (tasteMatcher.combina(prefs, r.getMovie()) ? combinam : resto).add(r);
        }
        if (combinam.size() >= minimo) return escolhidos;

        Set<Long> excluir = new HashSet<>(jaRegistrados);
        escolhidos.forEach(r -> excluir.add(r.getMovie().getId()));
        List<RecommendationResult> juntos = new ArrayList<>(combinam);
        juntos.addAll(porGosto(prefs, grafo, excluir, minimo - combinam.size()));
        juntos.addAll(resto);
        // nova passada de variedade: os do questionário podem repetir diretor com os do grafo
        return variados(juntos, jaRegistrados);
    }

    /** os filmes que mais combinam com as respostas, com "porque você curte ..." de motivo */
    private List<RecommendationResult> porGosto(Preferencias prefs, MovieGraph grafo, Set<Long> excluir, int quantos) {
        List<Movie> filmes = tasteMatcher.melhores(prefs, grafo.getAllMovies(), excluir, quantos);
        List<RecommendationResult> resultados = new ArrayList<>(filmes.size());
        for (int i = 0; i < filmes.size(); i++) {
            Movie filme = filmes.get(i);
            List<String> motivos = tasteMatcher.motivos(prefs, filme).stream()
                    .map(m -> "porque você curte " + m)
                    .toList();
            // pontuação só para manter a ordem; o front não mostra
            resultados.add(new RecommendationResult(filme, filmes.size() - i, motivos));
        }
        return resultados;
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
