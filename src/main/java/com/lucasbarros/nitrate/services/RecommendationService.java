package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.RecommendationRequestDTO;
import com.lucasbarros.nitrate.dto.RecommendationResultDTO;
import com.lucasbarros.nitrate.entities.DismissedRecommendation;
import com.lucasbarros.nitrate.entities.FavoritePerson;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.TasteProfile;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.graph.ConnectionType;
import com.lucasbarros.nitrate.graph.MovieGraph;
import com.lucasbarros.nitrate.graph.MovieGraphCache;
import com.lucasbarros.nitrate.recommender.RecommendationEngine;
import com.lucasbarros.nitrate.recommender.RecommendationResult;
import com.lucasbarros.nitrate.recommender.UserPreferences;
import com.lucasbarros.nitrate.repositories.DismissedRecommendationRepository;
import com.lucasbarros.nitrate.repositories.FavoritePersonRepository;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.repositories.TasteProfileRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import com.lucasbarros.nitrate.taste.TasteMatcher;
import com.lucasbarros.nitrate.taste.TasteMatcher.Preferencias;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RecommendationService {
    private static final int PREFERENCIAS_MAX = 200; // por lista, no POST /recommendations
    private static final int TOP_N_PADRAO = 5;
    private static final int TOP_N_USUARIO = 12;
    private static final double NOTA_MINIMA_SEMENTE = 3.5;
    private static final int MAX_SEMENTES = 5;
    // nota de até 2 (sem curtida) é rejeição: pesa contra os parecidos
    private static final double NOTA_MAXIMA_REJEICAO = 2.0;
    private static final int MAX_REJEITADOS = 10;
    private static final int MAX_DISPENSADOS_COMO_SINAL = 20;
    private static final int MAX_POR_DIRETOR = 2;
    private static final int MAX_POR_FRANQUIA = 2;
    // a semente vista hoje vale o dobro da vista há muito tempo; a diferença cai pela metade em ~4 meses
    private static final double DIAS_RECENCIA = 180;
    private static final double PESO_SEMENTE_DO_QUESTIONARIO = 0.8;
    private static final double PESO_SEMENTE_SEM_NOTA = 0.8;
    private static final Duration VALIDADE_CACHE = Duration.ofMinutes(10);
    // empurrão para os filmes que combinam com as respostas do questionário de gostos
    private static final double BONUS_GOSTO = 1.25;

    /** uma linha do diário, só com o que a recomendação usa */
    public record Registro(Long movieId, Double rating, boolean liked, LocalDate watchedOn, Instant createdAt) {
        LocalDate data() {
            return watchedOn != null ? watchedOn : createdAt.atZone(ZoneOffset.UTC).toLocalDate();
        }

        boolean positivo() {
            return liked || (rating != null && rating >= NOTA_MINIMA_SEMENTE);
        }
    }

    public record Favorito(boolean ator, String nome) {}

    /** tudo o que a recomendação precisa saber da pessoa, já lido do banco */
    public record Entrada(Long userId, List<Registro> registros, List<Long> sementesDoQuestionario,
                          Preferencias gosto, List<Favorito> favoritos, List<Long> dispensados) {}

    /** o que pode ser desligado na medição; na home vai tudo ligado */
    public record Ajustes(RecommendationEngine.Opcoes engine, Set<ConnectionType> tiposDesligados,
                          boolean ignorarCurtidasDaPessoa) {
        public static final Ajustes PADRAO = new Ajustes(RecommendationEngine.Opcoes.PADRAO, Set.of(), false);
    }

    private record RecomendacoesEmCache(List<RecommendationResultDTO> resultado, Instant em) {}

    private final Map<Long, RecomendacoesEmCache> cache = new ConcurrentHashMap<>();

    @Autowired
    private MovieGraphCache graphCache;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private FavoritePersonRepository favoritePersonRepository;

    @Autowired
    private TasteProfileRepository tasteProfileRepository;

    @Autowired
    private DismissedRecommendationRepository dismissedRepository;

    @Autowired
    private TasteMatcher tasteMatcher;

    /**
     * Recomendações a partir do diário: os últimos filmes que a pessoa curtiu ou avaliou bem
     * são as sementes do grafo. Se não houver nenhum, usa os últimos registrados sem nota. Tudo
     * que ela já registrou (inclusive o histórico antigo) e o que ainda não estreou ficam de fora.
     */
    public List<RecommendationResultDTO> recomendarParaUsuario(User user) {
        // a home pede isso em toda visita: guardamos o resultado por alguns minutos,
        // e qualquer mudança no diário da pessoa descarta o que estava guardado
        RecomendacoesEmCache emCache = cache.get(user.getId());
        if (emCache != null && emCache.em().plus(VALIDADE_CACHE).isAfter(Instant.now())) {
            return emCache.resultado();
        }
        List<RecommendationResultDTO> resultado = calcular(lerEntrada(user), Ajustes.PADRAO, graphCache.obter()).stream()
                .map(RecommendationResultDTO::new)
                .toList();
        Instant agora = Instant.now();
        // quem não voltou deixa o resultado vencido para trás: limpa aqui para o mapa não crescer para sempre
        cache.values().removeIf(c -> c.em().plus(VALIDADE_CACHE).isBefore(agora));
        cache.put(user.getId(), new RecomendacoesEmCache(resultado, agora));
        return resultado;
    }

    /** o diário ou os favoritos da pessoa mudaram: a próxima visita recalcula as recomendações */
    public void invalidar(Long userId) {
        cache.remove(userId);
    }

    /** "não tenho interesse": o filme some das recomendações e pesa contra os parecidos com ele */
    @Transactional
    public void dispensar(User user, Long movieId) {
        Movie filme = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Filme não encontrado. Id: " + movieId));
        if (!dismissedRepository.existsByUserAndMovie(user, filme)) {
            dismissedRepository.save(new DismissedRecommendation(user, filme));
        }
        invalidar(user.getId());
    }

    @Transactional
    public void desfazerDispensa(User user, Long movieId) {
        dismissedRepository.deleteByUserAndMovieId(user, movieId);
        invalidar(user.getId());
    }

    private Entrada lerEntrada(User user) {
        List<Registro> registros = new ArrayList<>();
        for (Object[] linha : reviewRepository.findRegistrosParaRecomendacao(user)) {
            registros.add(new Registro((Long) linha[0], (Double) linha[1], Boolean.TRUE.equals(linha[2]),
                    (LocalDate) linha[3], (Instant) linha[4]));
        }
        TasteProfile gosto = tasteProfileRepository.findById(user.getId()).orElse(null);
        List<Favorito> favoritos = favoritePersonRepository.findByUserOrderByPositionAsc(user).stream()
                .map(f -> new Favorito(f.getKind() == FavoritePerson.Kind.ACTOR, f.getName()))
                .toList();
        return new Entrada(user.getId(), registros,
                gosto == null ? List.of() : new ArrayList<>(gosto.getSeedMovieIds()),
                gosto == null ? null : Preferencias.de(gosto.getGenres(), gosto.getAnswers()),
                favoritos,
                dismissedRepository.findMovieIdsByUser(user));
    }

    /** o cálculo inteiro, sem banco: a home e a medição (RecommendationEvaluationService) passam por aqui */
    public List<RecommendationResult> calcular(Entrada entrada, Ajustes ajustes, MovieGraph grafo) {
        // do mais recente para o mais antigo pela data em que assistiu (o import do Letterboxd
        // cria tudo no mesmo instante, então a data de registro não diz nada); sem data, vai pro fim
        List<Registro> registros = new ArrayList<>(entrada.registros());
        registros.sort(Comparator.comparing(Registro::watchedOn, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Registro::createdAt, Comparator.reverseOrder()));

        LocalDate hoje = LocalDate.now();
        Set<Long> jaRegistrados = new HashSet<>();
        Map<Long, Double> sementes = new LinkedHashMap<>();
        Map<Long, Double> semNota = new LinkedHashMap<>();
        List<Long> rejeitados = new ArrayList<>();
        for (Registro registro : registros) {
            Long id = registro.movieId();
            jaRegistrados.add(id);
            // só os últimos filmes de que a pessoa gostou guiam o grafo: o gosto do momento,
            // não o histórico inteiro (que deixava tudo "reforçado por 86 filmes curtidos")
            if (registro.positivo()) {
                if (sementes.size() < MAX_SEMENTES) sementes.putIfAbsent(id, pesoDaSemente(registro, hoje));
            } else if (registro.rating() == null) {
                if (semNota.size() < MAX_SEMENTES) semNota.putIfAbsent(id, PESO_SEMENTE_SEM_NOTA * recencia(registro, hoje));
            } else if (registro.rating() <= NOTA_MAXIMA_REJEICAO && rejeitados.size() < MAX_REJEITADOS) {
                rejeitados.add(id);
            }
        }
        // um filme reassistido e curtido da segunda vez não é rejeição
        rejeitados.removeAll(sementes.keySet());
        // quantas curtidas de verdade guiam o grafo: com poucas, as respostas do questionário mandam mais
        int curtidasReais = sementes.size();

        // questionário do onboarding: os filmes que a pessoa marcou completam as vagas de semente.
        // Conforme ela curte filmes de verdade, eles perdem espaço até sumirem
        Preferencias prefs = entrada.gosto();
        boolean temGosto = prefs != null && !prefs.vazia();
        // disse que já viu: não faz sentido recomendar
        jaRegistrados.addAll(entrada.sementesDoQuestionario());
        for (Long id : entrada.sementesDoQuestionario()) {
            if (sementes.size() >= MAX_SEMENTES) break;
            sementes.putIfAbsent(id, PESO_SEMENTE_DO_QUESTIONARIO);
        }
        if (sementes.isEmpty()) {
            sementes = semNota;
        }

        // "não tenho interesse": some do resultado, e os mais recentes pesam contra os parecidos
        jaRegistrados.addAll(entrada.dispensados());
        rejeitados.addAll(entrada.dispensados().subList(0, Math.min(MAX_DISPENSADOS_COMO_SINAL, entrada.dispensados().size())));

        UserPreferences preferencias = new UserPreferences();
        for (Map.Entry<Long, Double> semente : sementes.entrySet()) {
            Movie filme = grafo.getMovie(semente.getKey());
            if (filme != null) {
                preferencias.addLikedMovie(filme, semente.getValue());
            }
        }
        if (preferencias.getLikedMovies().isEmpty()) {
            // nenhum filme para guiar o grafo: se respondeu o questionário, vão os que mais combinam
            if (!temGosto) return new ArrayList<>();
            return porGosto(prefs, grafo, comNaoLancados(jaRegistrados, grafo), TOP_N_USUARIO);
        }
        for (Long id : rejeitados) {
            Movie filme = grafo.getMovie(id);
            if (filme != null) preferencias.addDislikedMovie(filme);
        }
        // atores e diretores favoritos do perfil: filmes ligados a eles ganham peso extra
        for (Favorito favorito : entrada.favoritos()) {
            if (favorito.ator()) {
                preferencias.addFavoriteActor(favorito.nome());
            } else {
                preferencias.addFavoriteDirector(favorito.nome());
            }
        }
        for (ConnectionType tipo : ajustes.tiposDesligados()) {
            preferencias.setConnectionTypeMultipliers(tipo, 0);
        }
        if (ajustes.ignorarCurtidasDaPessoa()) {
            preferencias.setIgnoredUserId(entrada.userId());
        }

        // pede mais que o necessário porque os já registrados (mas não curtidos), os que ainda não
        // estrearam e os que estourarem o limite por diretor/franquia são descartados
        List<RecommendationResult> resultados = new RecommendationEngine(grafo, ajustes.engine())
                .recommend(preferencias, TOP_N_USUARIO * 6 + jaRegistrados.size());
        resultados = resultados.stream().filter(r -> r.getMovie().jaEstreou()).toList();
        if (temGosto) {
            resultados = comEmpurrao(resultados, prefs, curtidasReais);
        }

        List<RecommendationResult> escolhidos = variados(resultados, jaRegistrados);
        if (temGosto && curtidasReais < MAX_SEMENTES) {
            escolhidos = garantirGosto(escolhidos, prefs, grafo, comNaoLancados(jaRegistrados, grafo));
        }
        return escolhidos;
    }

    /**
     * Quanto uma semente pesa: a nota (5 estrelas vale mais que 3,5; a curtida soma) e quão recente
     * ela é (o filme de ontem diz mais do gosto de agora que o de dois anos atrás).
     */
    static double pesoDaSemente(Registro registro, LocalDate hoje) {
        double nota = registro.rating() != null ? 0.4 + 0.15 * registro.rating() : 1.0; // 3,5 → 0,93; 5 → 1,15
        if (registro.liked() && registro.rating() != null) nota += 0.1;
        return nota * recencia(registro, hoje);
    }

    private static double recencia(Registro registro, LocalDate hoje) {
        long dias = Math.max(0, ChronoUnit.DAYS.between(registro.data(), hoje));
        return 0.5 + 0.5 * Math.exp(-dias / DIAS_RECENCIA);
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
                                                     MovieGraph grafo, Set<Long> foraDoResultado) {
        int minimo = TOP_N_USUARIO / 2;
        List<RecommendationResult> combinam = new ArrayList<>();
        List<RecommendationResult> resto = new ArrayList<>();
        for (RecommendationResult r : escolhidos) {
            (tasteMatcher.combina(prefs, r.getMovie()) ? combinam : resto).add(r);
        }
        if (combinam.size() >= minimo) return escolhidos;

        Set<Long> excluir = new HashSet<>(foraDoResultado);
        escolhidos.forEach(r -> excluir.add(r.getMovie().getId()));
        List<RecommendationResult> juntos = new ArrayList<>(combinam);
        juntos.addAll(porGosto(prefs, grafo, excluir, minimo - combinam.size()));
        juntos.addAll(resto);
        // nova passada de variedade: os do questionário podem repetir diretor com os do grafo
        return variados(juntos, foraDoResultado);
    }

    /** os já registrados mais os que ainda não estrearam: o questionário só sugere o que dá para ver */
    private static Set<Long> comNaoLancados(Set<Long> jaRegistrados, MovieGraph grafo) {
        Set<Long> excluir = new HashSet<>(jaRegistrados);
        for (Movie filme : grafo.getAllMovies()) {
            if (!filme.jaEstreou()) excluir.add(filme.getId());
        }
        return excluir;
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
        // listas enormes fariam o motor percorrer o grafo a partir de cada item
        if (tamanho(request.getLikedMovieIds()) > PREFERENCIAS_MAX || tamanho(request.getFavoriteDirectors()) > PREFERENCIAS_MAX
                || tamanho(request.getFavoriteActors()) > PREFERENCIAS_MAX) {
            throw new IllegalArgumentException("Envie no máximo " + PREFERENCIAS_MAX + " itens em cada lista");
        }
        // o grafo vem do cache; os filmes curtidos precisam ser as mesmas instâncias que estão nele
        MovieGraph grafo = graphCache.obter();

        UserPreferences preferencias = montarPreferencias(request, grafo);

        RecommendationEngine engine = new RecommendationEngine(grafo);
        List<RecommendationResult> resultados = engine.recommend(preferencias, TOP_N_PADRAO);

        List<RecommendationResultDTO> dtos = new ArrayList<>();
        for (RecommendationResult resultado : resultados) {
            dtos.add(new RecommendationResultDTO(resultado));
        }
        return dtos;
    }

    private static int tamanho(List<?> lista) {
        return lista == null ? 0 : lista.size();
    }

    private UserPreferences montarPreferencias(RecommendationRequestDTO request, MovieGraph grafo) {
        UserPreferences preferencias = new UserPreferences();

        for (Long id : request.getLikedMovieIds()) {
            Movie filme = grafo.getMovie(id);
            if (filme == null) {
                throw new ResourceNotFoundException("Filme não encontrado. Id: " + id);
            }
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
}
