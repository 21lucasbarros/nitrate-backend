package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.MovieBackdropsDTO;
import com.lucasbarros.nitrate.dto.MovieStatsDTO;
import com.lucasbarros.nitrate.dto.PageDTO;
import com.lucasbarros.nitrate.dto.SeasonDTO;
import com.lucasbarros.nitrate.dto.SeriesDTO;
import com.lucasbarros.nitrate.dto.SeriesDetailsDTO;
import com.lucasbarros.nitrate.dto.SeriesSearchResultDTO;
import com.lucasbarros.nitrate.dto.WatchProvidersDTO;
import com.lucasbarros.nitrate.entities.Series;
import com.lucasbarros.nitrate.entities.SeriesReview;
import com.lucasbarros.nitrate.repositories.SeriesRepository;
import com.lucasbarros.nitrate.repositories.SeriesReviewRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import com.lucasbarros.nitrate.tmdb.TmdbClient;
import com.lucasbarros.nitrate.tmdb.TmdbImportService;
import com.lucasbarros.nitrate.tmdb.dto.TmdbGenreDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbTvDetailDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbTvSeasonDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbTvSummaryDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbWatchProvidersDTO;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.text.Collator;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Séries de TV, com os dados do TMDB (a mesma API gratuita dos filmes). Não se mistura com os
 * filmes: tabela, reviews e watchlist próprias, e nada daqui entra no grafo ou nas recomendações.
 */
@Service
public class SeriesService {

    private static final int TERMO_MIN = 2;
    private static final int MAX_ELENCO = 30;
    private static final int LISTA_MAX = 18;
    private static final int CATALOGO_PAGINA_MAX = 60;
    private static final int COMUNIDADE_MAX = 12;
    private static final int COMUNIDADE_MINIMO = 6; // menos que isso em alta: usa o histórico todo
    private static final long EM_ALTA_DIAS = 30;
    private static final int SLUG_TAMANHO_MAX = 80;
    private static final String PAIS = "BR";
    private static final Duration VALIDADE_LISTAS = Duration.ofHours(6);
    private static final Duration VALIDADE_DETALHES = Duration.ofHours(24);
    private static final Duration VALIDADE_PROVEDORES = Duration.ofHours(12);

    @Autowired private SeriesRepository seriesRepository;
    @Autowired private SeriesReviewRepository seriesReviewRepository;
    @Autowired private TmdbClient tmdbClient;
    @Autowired private PersonSlugService personSlugService;
    @Autowired private EntityManager entityManager;

    // mesma ideia dos filmes: duas aberturas simultâneas da mesma série não importam em dobro
    private final ReentrantLock importacao = new ReentrantLock();

    private record EmCache<T>(T valor, Instant em) {
        boolean valido(Duration validade) {
            return em.plus(validade).isAfter(Instant.now());
        }
    }

    private final Map<String, EmCache<List<SeriesSearchResultDTO>>> cacheListas = new ConcurrentHashMap<>();
    private final Map<Long, EmCache<SeriesDetailsDTO>> cacheDetalhes = new ConcurrentHashMap<>();
    private final Map<String, EmCache<SeasonDTO>> cacheTemporadas = new ConcurrentHashMap<>();
    private final Map<Long, EmCache<WatchProvidersDTO>> cacheProvedores = new ConcurrentHashMap<>();
    private final Map<Long, EmCache<MovieBackdropsDTO>> cacheCenas = new ConcurrentHashMap<>();

    // sem @Transactional para não segurar uma conexão do banco enquanto espera o TMDB
    public List<SeriesSearchResultDTO> pesquisar(String termo) {
        if (termo == null || termo.trim().length() < TERMO_MIN) return List.of();
        return comIdsLocais(tmdbClient.pesquisarSeries(termo.trim()).getResults());
    }

    public List<SeriesSearchResultDTO> emAlta() {
        return lista("em-alta", () -> tmdbClient.seriesEmAlta().getResults());
    }

    public List<SeriesSearchResultDTO> maisBemAvaliadas() {
        return lista("mais-bem-avaliadas", () -> tmdbClient.seriesMaisBemAvaliadas().getResults());
    }

    /** as séries que mais gente registrou nos últimos dias; com pouca atividade recente, de todos os tempos (como nos filmes) */
    @Transactional(readOnly = true)
    public List<SeriesSearchResultDTO> emAltaNaComunidade() {
        PageRequest pagina = PageRequest.of(0, COMUNIDADE_MAX);
        List<Object[]> linhas = seriesReviewRepository.findMaisRegistradas(
                Instant.now().minus(Duration.ofDays(EM_ALTA_DIAS)), pagina);
        if (linhas.size() < COMUNIDADE_MINIMO) {
            linhas = seriesReviewRepository.findMaisRegistradas(Instant.EPOCH, pagina);
        }
        List<Long> ids = linhas.stream().map(l -> ((Number) l[0]).longValue()).toList();
        Map<Long, Series> porId = new HashMap<>();
        seriesRepository.findAllById(ids).forEach(s -> porId.put(s.getId(), s));
        return ids.stream().map(porId::get).filter(Objects::nonNull).map(SeriesSearchResultDTO::new).toList();
    }

    /** as séries que já estão no Nitrate, em páginas, com os mesmos filtros do catálogo de filmes */
    @Transactional(readOnly = true)
    public PageDTO<SeriesSearchResultDTO> catalogo(int pagina, int tamanho, CatalogoFiltro filtro) {
        int numero = Math.max(0, pagina);
        int porPagina = Math.min(Math.max(1, tamanho), CATALOGO_PAGINA_MAX);
        CatalogoConsulta.Pagina resultado = CatalogoConsulta.SERIES.buscar(entityManager, filtro, numero, porPagina);

        Map<Long, Series> porId = new HashMap<>();
        seriesRepository.findAllById(resultado.ids()).forEach(s -> porId.put(s.getId(), s));
        List<SeriesSearchResultDTO> itens = resultado.ids().stream().map(porId::get).filter(Objects::nonNull)
                .map(SeriesSearchResultDTO::new).toList();
        int paginas = (int) Math.ceil((double) resultado.total() / porPagina);
        return new PageDTO<>(itens, numero, porPagina, resultado.total(), paginas);
    }

    /** os gêneros que aparecem no catálogo de séries, em ordem alfabética (para o filtro) */
    @Transactional(readOnly = true)
    public List<String> generos() {
        Collator collator = Collator.getInstance(Locale.of("pt", "BR"));
        return seriesRepository.findNomesDeGeneros().stream().sorted(collator).toList();
    }

    private List<SeriesSearchResultDTO> lista(String chave, Supplier<List<TmdbTvSummaryDTO>> buscar) {
        EmCache<List<SeriesSearchResultDTO>> emCache = cacheListas.get(chave);
        if (emCache != null && emCache.valido(VALIDADE_LISTAS)) return emCache.valor();
        List<TmdbTvSummaryDTO> resultados = buscar.get();
        List<SeriesSearchResultDTO> dtos = comIdsLocais(resultados.subList(0, Math.min(LISTA_MAX, resultados.size())));
        CacheLimitado.guardar(cacheListas, chave, new EmCache<>(dtos, Instant.now()));
        return dtos;
    }

    // as séries que já estão no banco vêm com o id e o slug locais
    private List<SeriesSearchResultDTO> comIdsLocais(List<TmdbTvSummaryDTO> encontradas) {
        List<Long> tmdbIds = encontradas.stream().map(TmdbTvSummaryDTO::getId).toList();
        Map<Long, Series> localPorTmdbId = new HashMap<>();
        for (Series serie : seriesRepository.findByTmdbIdIn(tmdbIds)) {
            localPorTmdbId.put(serie.getTmdbId(), serie);
        }
        List<SeriesSearchResultDTO> resultados = new ArrayList<>();
        for (TmdbTvSummaryDTO resumo : encontradas) {
            resultados.add(new SeriesSearchResultDTO(resumo, localPorTmdbId.get(resumo.getId())));
        }
        return resultados;
    }

    @Transactional(readOnly = true)
    public SeriesDTO buscarPorId(Long id) {
        return new SeriesDTO(buscar(id));
    }

    @Transactional(readOnly = true)
    public SeriesDTO buscarPorSlug(String slug) {
        return seriesRepository.findBySlug(slug)
                .map(SeriesDTO::new)
                .orElseThrow(() -> new ResourceNotFoundException("Série não encontrada: " + slug));
    }

    /**
     * Id da série local do tmdbId, importando do TMDB na primeira vez. Devolve só o id: fora de
     * uma transação os gêneros (carregados sob demanda) não podem ser lidos.
     */
    public Long obterOuImportarId(Long tmdbId) {
        importacao.lock();
        try {
            Series serie = seriesRepository.findByTmdbId(tmdbId).orElse(null);
            if (serie == null) {
                TmdbTvDetailDTO detalhe = buscarNoTmdb(tmdbId);
                serie = new Series();
                serie.setTmdbId(tmdbId);
                preencher(serie, detalhe);
                serie.setSlug(MovieSlugService.livre(slugBase(serie), seriesRepository::existsBySlug));
                serie = seriesRepository.save(serie);
            }
            return serie.getId();
        } finally {
            importacao.unlock();
        }
    }

    private TmdbTvDetailDTO buscarNoTmdb(Long tmdbId) {
        try {
            return tmdbClient.buscarSerie(tmdbId);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Série não encontrada no TMDB. Id: " + tmdbId);
        }
    }

    private static void preencher(Series serie, TmdbTvDetailDTO d) {
        serie.setName(d.getName() == null || d.getName().isBlank() ? "Série " + d.getId() : d.getName());
        serie.setFirstAirYear(SeriesSearchResultDTO.ano(d.getFirstAirDate()));
        serie.setFirstAirDate(TmdbImportService.extrairData(d.getFirstAirDate()));
        serie.setLastAirYear(emExibicao(d) ? null : SeriesSearchResultDTO.ano(d.getLastAirDate()));
        serie.setStatus(d.getStatus());
        serie.setNumberOfSeasons(d.getNumberOfSeasons());
        serie.setNumberOfEpisodes(d.getNumberOfEpisodes());
        serie.setRating(d.getVoteAverage());
        String sinopse = d.getOverview() == null || d.getOverview().isBlank() ? null : d.getOverview();
        serie.setOverview(sinopse != null && sinopse.length() > 1000 ? sinopse.substring(0, 997) + "..." : sinopse);
        serie.setPosterPath(d.getPosterPath());
        String criadores = d.getCreatedBy().stream().map(TmdbTvDetailDTO.Creator::getName)
                .filter(Objects::nonNull).collect(Collectors.joining(", "));
        serie.setCreators(criadores.isEmpty() ? null : criadores);
        serie.setGenres(new ArrayList<>(d.getGenres().stream().map(TmdbGenreDTO::getName).filter(Objects::nonNull).toList()));
    }

    /** a série segue no ar (ou volta com temporada nova) */
    static boolean emExibicao(TmdbTvDetailDTO d) {
        return d.isInProduction() || "Returning Series".equals(d.getStatus());
    }

    private static String slugBase(Series serie) {
        String texto = MovieSlugService.slugify(serie.getName(), SLUG_TAMANHO_MAX);
        if (texto.isEmpty()) texto = "serie-" + serie.getTmdbId();
        return serie.getFirstAirYear() == null ? texto : texto + "-" + serie.getFirstAirYear();
    }

    /**
     * Temporadas, criadores, elenco e imagem de fundo, direto do TMDB (guardados por um dia).
     * Aproveita a consulta para atualizar no banco o que muda com o tempo (temporadas, status, nota).
     */
    public SeriesDetailsDTO detalhes(Long id) {
        Series serie = seriesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Série não encontrada. Id: " + id));
        EmCache<SeriesDetailsDTO> emCache = cacheDetalhes.get(serie.getTmdbId());
        if (emCache != null && emCache.valido(VALIDADE_DETALHES)) return emCache.valor();

        TmdbTvDetailDTO d = buscarNoTmdb(serie.getTmdbId());
        String slug = serie.getSlug();
        preencher(serie, d);
        serie.setSlug(slug); // o endereço não muda mesmo que o nome mude no TMDB
        seriesRepository.save(serie);

        List<TmdbTvDetailDTO.CastMember> principais = new ArrayList<>();
        if (d.getAggregateCredits() != null) {
            List<TmdbTvDetailDTO.CastMember> ordenado = new ArrayList<>(d.getAggregateCredits().getCast());
            ordenado.sort(Comparator.comparing(m -> m.getOrder() == null ? Integer.MAX_VALUE : m.getOrder()));
            principais = ordenado.subList(0, Math.min(MAX_ELENCO, ordenado.size()));
        }

        // o link de cada pessoa usa o slug ("/person/bryan-cranston")
        Map<Long, String> nomes = new HashMap<>();
        d.getCreatedBy().forEach(c -> nomes.put(c.getId(), c.getName()));
        principais.forEach(m -> nomes.put(m.getId(), m.getName()));
        Map<Long, String> slugs = personSlugService.slugs(nomes);

        List<SeriesDetailsDTO.Person> criadores = d.getCreatedBy().stream()
                .map(c -> new SeriesDetailsDTO.Person(c.getId(), c.getName(), null, c.getProfilePath(), null,
                        slugs.get(c.getId())))
                .toList();

        List<SeriesDetailsDTO.Person> elenco = new ArrayList<>();
        for (TmdbTvDetailDTO.CastMember m : principais) {
            String papeis = m.getRoles().stream().map(TmdbTvDetailDTO.Role::getCharacter)
                    .filter(c -> c != null && !c.isBlank()).distinct().limit(2)
                    .collect(Collectors.joining(" / "));
            elenco.add(new SeriesDetailsDTO.Person(m.getId(), m.getName(), papeis.isEmpty() ? null : papeis,
                    m.getProfilePath(), m.getTotalEpisodeCount(), slugs.get(m.getId())));
        }

        // especiais (temporada 0) por último
        List<SeriesDetailsDTO.Season> temporadas = d.getSeasons().stream()
                .filter(t -> t.getSeasonNumber() != null)
                .sorted(Comparator.comparing((TmdbTvDetailDTO.Season t) -> t.getSeasonNumber() == 0)
                        .thenComparing(TmdbTvDetailDTO.Season::getSeasonNumber))
                .map(t -> new SeriesDetailsDTO.Season(t.getSeasonNumber(), t.getName(), vazioParaNulo(t.getOverview()),
                        t.getEpisodeCount(), t.getAirDate(), t.getPosterPath(),
                        t.getVoteAverage() == null || t.getVoteAverage() == 0 ? null : t.getVoteAverage()))
                .toList();

        TmdbTvDetailDTO.Episode proximo = d.getNextEpisodeToAir();
        SeriesDetailsDTO resultado = new SeriesDetailsDTO(
                d.getBackdropPath(),
                vazioParaNulo(d.getTagline()),
                d.getStatus(),
                emExibicao(d),
                d.getFirstAirDate(),
                d.getLastAirDate(),
                d.getEpisodeRunTime().isEmpty() ? null : d.getEpisodeRunTime().get(0),
                criadores,
                d.getNetworks().stream().map(TmdbTvDetailDTO.Network::getName).filter(Objects::nonNull).toList(),
                temporadas,
                proximo == null ? null : new SeriesDetailsDTO.NextEpisode(proximo.getName(), proximo.getAirDate(),
                        proximo.getSeasonNumber(), proximo.getEpisodeNumber()),
                elenco);
        CacheLimitado.guardar(cacheDetalhes, serie.getTmdbId(), new EmCache<>(resultado, Instant.now()));
        return resultado;
    }

    public SeasonDTO temporada(Long id, int numero) {
        Series serie = buscar(id);
        String chave = serie.getTmdbId() + ":" + numero;
        EmCache<SeasonDTO> emCache = cacheTemporadas.get(chave);
        if (emCache != null && emCache.valido(VALIDADE_DETALHES)) return emCache.valor();

        TmdbTvSeasonDTO t;
        try {
            t = tmdbClient.buscarTemporada(serie.getTmdbId(), numero);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Temporada não encontrada: " + numero);
        }
        List<SeasonDTO.Episode> episodios = t.getEpisodes().stream()
                .filter(e -> e.getEpisodeNumber() != null)
                .map(e -> new SeasonDTO.Episode(e.getEpisodeNumber(), e.getName(), vazioParaNulo(e.getOverview()),
                        e.getAirDate(), e.getRuntime(), e.getStillPath(),
                        e.getVoteAverage() == null || e.getVoteAverage() == 0 ? null : e.getVoteAverage()))
                .toList();
        SeasonDTO resultado = new SeasonDTO(numero, t.getName(), vazioParaNulo(t.getOverview()), t.getAirDate(),
                t.getPosterPath(), episodios);
        CacheLimitado.guardar(cacheTemporadas, chave, new EmCache<>(resultado, Instant.now()));
        return resultado;
    }

    /** as cenas da série, para o fundo da imagem dos Stories (no mesmo formato das cenas de filme) */
    public MovieBackdropsDTO cenas(Long id) {
        Series serie = buscar(id);
        EmCache<MovieBackdropsDTO> emCache = cacheCenas.get(serie.getTmdbId());
        if (emCache != null && emCache.valido(VALIDADE_DETALHES)) return emCache.valor();

        // a cena principal (a da página da série) entra mesmo que o TMDB não a liste entre as imagens
        String principal = detalhes(id).backdropPath();
        List<String> caminhos = ProfileCoverService.ordenarCenas(tmdbClient.buscarImagensSerie(serie.getTmdbId()), principal);
        MovieBackdropsDTO resultado = new MovieBackdropsDTO(serie.getTmdbId(), serie.getName(), serie.getFirstAirYear(), caminhos);
        CacheLimitado.guardar(cacheCenas, serie.getTmdbId(), new EmCache<>(resultado, Instant.now()));
        return resultado;
    }

    public WatchProvidersDTO ondeAssistir(Long id) {
        Series serie = buscar(id);
        EmCache<WatchProvidersDTO> emCache = cacheProvedores.get(serie.getTmdbId());
        if (emCache != null && emCache.valido(VALIDADE_PROVEDORES)) return emCache.valor();

        TmdbWatchProvidersDTO.Country brasil = tmdbClient.buscarOndeAssistirSerie(serie.getTmdbId()).getResults().get(PAIS);
        WatchProvidersDTO resultado = brasil == null
                ? WatchProvidersDTO.vazio()
                : new WatchProvidersDTO(
                        brasil.getLink(),
                        MovieService.provedores(brasil.getFlatrate()),
                        MovieService.provedores(MovieService.concat(brasil.getFree(), brasil.getAds())),
                        MovieService.provedores(brasil.getRent()),
                        MovieService.provedores(brasil.getBuy()));
        CacheLimitado.guardar(cacheProvedores, serie.getTmdbId(), new EmCache<>(resultado, Instant.now()));
        return resultado;
    }

    /**
     * Como a série foi recebida no Nitrate. Cada pessoa conta uma vez: a nota é a do registro mais
     * recente da série inteira; sem ele, a média das temporadas (a nota mais recente de cada uma).
     * Nota de episódio não entra: ela diz o que a pessoa achou do episódio, não da série. Quem só
     * registrou episódios conta em "assistiu" e na curtida, mas não na nota.
     */
    @Transactional(readOnly = true)
    public MovieStatsDTO estatisticas(Long id) {
        Series serie = buscar(id);
        Map<Long, List<SeriesReview>> porPessoa = new HashMap<>();
        for (SeriesReview review : seriesReviewRepository.findBySeriesOrderByCreatedAtDesc(serie)) {
            porPessoa.computeIfAbsent(review.getUser().getId(), k -> new ArrayList<>()).add(review);
        }

        Map<String, Integer> distribuicao = new LinkedHashMap<>();
        for (double nota = 0.5; nota <= 5.0; nota += 0.5) distribuicao.put(String.valueOf(nota), 0);
        int comNota = 0;
        int curtidas = 0;
        double soma = 0;
        for (List<SeriesReview> registros : porPessoa.values()) {
            if (registros.stream().anyMatch(SeriesReview::isLiked)) curtidas++;
            Double nota = notaDaSerie(registros);
            if (nota != null) {
                comNota++;
                soma += nota;
                distribuicao.merge(String.valueOf(nota), 1, Integer::sum);
            }
        }
        Double media = comNota == 0 ? null : Math.round(soma / comNota * 10) / 10.0;
        return new MovieStatsDTO(media, comNota, porPessoa.size(), curtidas, distribuicao);
    }

    // a nota que a pessoa deu para a série; a média das temporadas é arredondada para o meio ponto
    private static Double notaDaSerie(List<SeriesReview> registros) {
        Optional<SeriesReview> inteira = registros.stream()
                .filter(r -> r.getSeasonNumber() == null && r.getRating() != null)
                .max(MAIS_RECENTE);
        if (inteira.isPresent()) return inteira.get().getRating();

        Map<Integer, SeriesReview> porTemporada = new HashMap<>();
        for (SeriesReview r : registros) {
            if (r.getSeasonNumber() == null || r.getEpisodeNumber() != null || r.getRating() == null) continue;
            porTemporada.merge(r.getSeasonNumber(), r, (a, b) -> MAIS_RECENTE.compare(a, b) >= 0 ? a : b);
        }
        if (porTemporada.isEmpty()) return null;
        double media = porTemporada.values().stream().mapToDouble(SeriesReview::getRating).average().orElseThrow();
        return Math.round(media * 2) / 2.0;
    }

    private static final Comparator<SeriesReview> MAIS_RECENTE =
            Comparator.comparing(SeriesReview::getWatchedOn, Comparator.nullsFirst(Comparator.<LocalDate>naturalOrder()))
                    .thenComparing(SeriesReview::getCreatedAt);

    Series buscar(Long id) {
        return seriesRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Série não encontrada. Id: " + id));
    }

    private static String vazioParaNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto;
    }
}
