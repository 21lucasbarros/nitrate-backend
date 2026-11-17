package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.MovieCreditsDTO;
import com.lucasbarros.nitrate.dto.MovieDTO;
import com.lucasbarros.nitrate.dto.MovieSearchResultDTO;
import com.lucasbarros.nitrate.dto.MovieStatsDTO;
import com.lucasbarros.nitrate.dto.PageDTO;
import com.lucasbarros.nitrate.dto.WatchProvidersDTO;
import com.lucasbarros.nitrate.graph.MovieGraphCache;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import com.lucasbarros.nitrate.tmdb.TmdbClient;
import com.lucasbarros.nitrate.tmdb.TmdbImportService;
import com.lucasbarros.nitrate.tmdb.dto.TmdbCastMemberDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbCreditsDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbCrewMemberDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbMovieDetailDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbMovieSummaryDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbWatchProvidersDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Service
public class MovieService {
    private static final int TERMO_MIN = 2;
    private static final int CATALOGO_PAGINA_MAX = 60;
    private static final int COMUNIDADE_MAX = 12;
    private static final int COMUNIDADE_MINIMO = 6; // menos que isso em alta: usa o histórico todo
    private static final long EM_ALTA_DIAS = 30;
    private static final long MELHORES_MINIMO_AVALIACOES = 2;

    @Autowired
    private MovieRepository movieRepository;

    private final ReentrantLock importacao = new ReentrantLock();

    @Autowired
    private TmdbClient tmdbClient;

    @Autowired
    private TmdbImportService tmdbImportService;

    @Autowired
    private MovieGraphCache graphCache;

    @Autowired
    private ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public List<MovieDTO> buscarTodos() {
        List<Movie> filmes = movieRepository.findAll();
        List<MovieDTO> dtos = new ArrayList<>();
        for(Movie filme : filmes) {
            dtos.add(new MovieDTO(filme));
        }
        return dtos;
    }

    /** os filmes que mais gente registrou nos últimos dias; com pouca atividade recente, de todos os tempos */
    @Transactional(readOnly = true)
    public List<MovieDTO> emAltaNaComunidade() {
        PageRequest pagina = PageRequest.of(0, COMUNIDADE_MAX);
        List<Object[]> linhas = reviewRepository.findMaisRegistrados(
                Instant.now().minus(Duration.ofDays(EM_ALTA_DIAS)), pagina);
        if (linhas.size() < COMUNIDADE_MINIMO) {
            linhas = reviewRepository.findMaisRegistrados(Instant.EPOCH, pagina);
        }
        return filmesNaOrdem(linhas);
    }

    /** as maiores notas médias entre filmes que várias pessoas avaliaram */
    @Transactional(readOnly = true)
    public List<MovieDTO> maisBemAvaliadosPelaComunidade() {
        return filmesNaOrdem(reviewRepository.findMaisBemAvaliados(
                MELHORES_MINIMO_AVALIACOES, PageRequest.of(0, COMUNIDADE_MAX)));
    }

    /** linhas [movieId, ...] → filmes na mesma ordem */
    private List<MovieDTO> filmesNaOrdem(List<Object[]> linhas) {
        List<Long> ids = linhas.stream().map(l -> ((Number) l[0]).longValue()).toList();
        Map<Long, Movie> porId = new HashMap<>();
        movieRepository.findAllById(ids).forEach(m -> porId.put(m.getId(), m));
        List<MovieDTO> dtos = new ArrayList<>();
        for (Long id : ids) {
            Movie filme = porId.get(id);
            if (filme != null) dtos.add(new MovieDTO(filme));
        }
        return dtos;
    }

    /** o catálogo em páginas, na ordem em que os filmes entraram no Nitrate */
    @Transactional(readOnly = true)
    public PageDTO<MovieDTO> catalogo(int pagina, int tamanho) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, pagina), Math.min(Math.max(1, tamanho), CATALOGO_PAGINA_MAX),
                Sort.by("id"));
        return PageDTO.of(movieRepository.findAll(pageRequest), MovieDTO::new);
    }

    @Transactional(readOnly = true)
    public MovieDTO buscarPorId(Long id) {
        Movie filme = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Filme não encontrado. Id: " + id));
        return new MovieDTO(filme);
    }

    // busca direto no TMDB; os filmes que já estão no banco vêm com o id local preenchido.
    // sem @Transactional para não segurar uma conexão do banco enquanto espera o TMDB
    public List<MovieSearchResultDTO> pesquisar(String termo) {
        List<MovieSearchResultDTO> resultados = new ArrayList<>();
        if (termo == null || termo.trim().length() < TERMO_MIN) {
            return resultados;
        }

        List<TmdbMovieSummaryDTO> encontrados = tmdbClient.pesquisarFilmes(termo.trim()).getResults();

        List<Long> tmdbIds = new ArrayList<>();
        for (TmdbMovieSummaryDTO resumo : encontrados) {
            tmdbIds.add(resumo.getId());
        }
        Map<Long, Movie> localPorTmdbId = new HashMap<>();
        for (Movie filme : movieRepository.findByTmdbIdIn(tmdbIds)) {
            localPorTmdbId.put(filme.getTmdbId(), filme);
        }

        for (TmdbMovieSummaryDTO resumo : encontrados) {
            resultados.add(new MovieSearchResultDTO(resumo, localPorTmdbId.get(resumo.getId())));
        }
        return resultados;
    }

    // trava: duas requisições simultâneas para o mesmo filme não podem importar em dobro
    // (o tmdbId é único). A importação roda e faz commit na sua própria transação.
    // ReentrantLock e não synchronized: com virtual threads, um synchronized que espera a rede
    // (a chamada ao TMDB) prende a thread do sistema e pode travar o servidor inteiro.
    public Long obterOuImportarId(Long tmdbId) {
        importacao.lock();
        try {
            boolean jaExistia = movieRepository.existsByTmdbId(tmdbId);
            Long id = tmdbImportService.obterOuImportar(tmdbId).getId();
            if (!jaExistia) {
                graphCache.invalidar(); // filme novo no acervo: o grafo precisa incluí-lo
            }
            return id;
        } finally {
            importacao.unlock();
        }
    }

    @Transactional(readOnly = true)
    public MovieDTO buscarPorSlug(String slug) {
        Movie filme = movieRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Filme não encontrado: " + slug));
        return new MovieDTO(filme);
    }

    // créditos mudam pouco: guardamos em memória por um dia para não consultar o TMDB a cada visita
    private static final Duration VALIDADE_CREDITOS = Duration.ofHours(24);
    private static final int MAX_ELENCO = 30;
    private final Map<Long, CreditosEmCache> cacheCreditos = new ConcurrentHashMap<>();

    private record CreditosEmCache(MovieCreditsDTO creditos, Instant em) {}

    public MovieCreditsDTO creditos(Long id) {
        Movie filme = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Filme não encontrado. Id: " + id));
        CreditosEmCache emCache = cacheCreditos.get(filme.getTmdbId());
        if (emCache != null && emCache.em().plus(VALIDADE_CREDITOS).isAfter(Instant.now())) {
            return emCache.creditos();
        }

        TmdbMovieDetailDTO detalhe = tmdbClient.buscarDetalhesComCreditos(filme.getTmdbId());
        TmdbCreditsDTO creditos = detalhe.getCredits();
        List<MovieCreditsDTO.Person> direcao = new ArrayList<>();
        List<MovieCreditsDTO.Person> elenco = new ArrayList<>();
        if (creditos != null) {
            for (TmdbCrewMemberDTO membro : creditos.getCrew()) {
                if ("Director".equals(membro.getJob())) {
                    direcao.add(new MovieCreditsDTO.Person(membro.getId(), membro.getName(), null, membro.getProfilePath()));
                }
            }
            List<TmdbCastMemberDTO> ordenado = new ArrayList<>(creditos.getCast());
            ordenado.sort(Comparator.comparing(m -> m.getOrder() == null ? Integer.MAX_VALUE : m.getOrder()));
            for (TmdbCastMemberDTO membro : ordenado.subList(0, Math.min(MAX_ELENCO, ordenado.size()))) {
                elenco.add(new MovieCreditsDTO.Person(membro.getId(), membro.getName(), membro.getCharacter(), membro.getProfilePath()));
            }
        }
        String tagline = detalhe.getTagline() == null || detalhe.getTagline().isBlank() ? null : detalhe.getTagline();
        MovieCreditsDTO resultado = new MovieCreditsDTO(direcao, elenco, detalhe.getBackdropPath(), tagline);
        cacheCreditos.put(filme.getTmdbId(), new CreditosEmCache(resultado, Instant.now()));
        return resultado;
    }

    @Transactional(readOnly = true)
    public MovieStatsDTO estatisticas(Long id) {
        Movie filme = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Filme não encontrado. Id: " + id));

        // a mais recente de cada pessoa (reassistidos não contam duas vezes)
        Map<Long, Review> ultimaPorPessoa = new HashMap<>();
        for (Review review : reviewRepository.findByMovieOrderByCreatedAtDesc(filme)) {
            ultimaPorPessoa.merge(review.getUser().getId(), review, (atual, outra) ->
                    maisRecente(outra, atual) ? outra : atual);
        }

        Map<String, Integer> distribuicao = new LinkedHashMap<>();
        for (double nota = 0.5; nota <= 5.0; nota += 0.5) distribuicao.put(String.valueOf(nota), 0);
        int comNota = 0;
        int curtidas = 0;
        double soma = 0;
        for (Review review : ultimaPorPessoa.values()) {
            if (review.isLiked()) curtidas++;
            if (review.getRating() != null) {
                comNota++;
                soma += review.getRating();
                distribuicao.merge(String.valueOf(review.getRating()), 1, Integer::sum);
            }
        }
        Double media = comNota == 0 ? null : Math.round(soma / comNota * 10) / 10.0;
        return new MovieStatsDTO(media, comNota, ultimaPorPessoa.size(), curtidas, distribuicao);
    }

    private static boolean maisRecente(Review a, Review b) {
        int porData = Comparator.nullsFirst(Comparator.<LocalDate>naturalOrder())
                .compare(a.getWatchedOn(), b.getWatchedOn());
        return porData != 0 ? porData > 0 : a.getCreatedAt().isAfter(b.getCreatedAt());
    }

    // disponibilidade muda (entra e sai de catálogo), mas não de hora em hora
    private static final Duration VALIDADE_PROVEDORES = Duration.ofHours(12);
    private static final String PAIS = "BR";
    private final Map<Long, ProvedoresEmCache> cacheProvedores = new ConcurrentHashMap<>();

    private record ProvedoresEmCache(WatchProvidersDTO provedores, Instant em) {}

    public WatchProvidersDTO ondeAssistir(Long id) {
        Movie filme = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Filme não encontrado. Id: " + id));
        ProvedoresEmCache emCache = cacheProvedores.get(filme.getTmdbId());
        if (emCache != null && emCache.em().plus(VALIDADE_PROVEDORES).isAfter(Instant.now())) {
            return emCache.provedores();
        }

        TmdbWatchProvidersDTO.Country brasil = tmdbClient.buscarOndeAssistir(filme.getTmdbId()).getResults().get(PAIS);
        WatchProvidersDTO resultado = brasil == null
                ? WatchProvidersDTO.vazio()
                : new WatchProvidersDTO(
                        brasil.getLink(),
                        provedores(brasil.getFlatrate()),
                        provedores(concat(brasil.getFree(), brasil.getAds())),
                        provedores(brasil.getRent()),
                        provedores(brasil.getBuy()));
        cacheProvedores.put(filme.getTmdbId(), new ProvedoresEmCache(resultado, Instant.now()));
        return resultado;
    }

    private static List<TmdbWatchProvidersDTO.Provider> concat(List<TmdbWatchProvidersDTO.Provider> a,
                                                             List<TmdbWatchProvidersDTO.Provider> b) {
        List<TmdbWatchProvidersDTO.Provider> todos = new ArrayList<>(a);
        todos.addAll(b);
        return todos;
    }

    // na ordem de destaque do JustWatch, sem repetir o mesmo serviço
    private static List<WatchProvidersDTO.Provider> provedores(List<TmdbWatchProvidersDTO.Provider> lista) {
        List<TmdbWatchProvidersDTO.Provider> ordenada = new ArrayList<>(lista);
        ordenada.sort(Comparator.comparingInt(TmdbWatchProvidersDTO.Provider::getDisplayPriority));
        Map<Long, WatchProvidersDTO.Provider> unicos = new LinkedHashMap<>();
        for (TmdbWatchProvidersDTO.Provider p : ordenada) {
            unicos.putIfAbsent(p.getProviderId(), new WatchProvidersDTO.Provider(p.getProviderId(), p.getProviderName(), p.getLogoPath()));
        }
        return new ArrayList<>(unicos.values());
    }
}
