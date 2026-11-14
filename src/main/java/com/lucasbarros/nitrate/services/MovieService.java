package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.MovieCreditsDTO;
import com.lucasbarros.nitrate.dto.MovieDTO;
import com.lucasbarros.nitrate.dto.MovieSearchResultDTO;
import com.lucasbarros.nitrate.graph.MovieGraphCache;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import com.lucasbarros.nitrate.tmdb.TmdbClient;
import com.lucasbarros.nitrate.tmdb.TmdbImportService;
import com.lucasbarros.nitrate.tmdb.dto.TmdbCastMemberDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbCreditsDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbCrewMemberDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbMovieSummaryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MovieService {
    private static final int TERMO_MIN = 2;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private TmdbClient tmdbClient;

    @Autowired
    private TmdbImportService tmdbImportService;

    @Autowired
    private MovieGraphCache graphCache;

    @Transactional(readOnly = true)
    public List<MovieDTO> buscarTodos() {
        List<Movie> filmes = movieRepository.findAll();
        List<MovieDTO> dtos = new ArrayList<>();
        for(Movie filme : filmes) {
            dtos.add(new MovieDTO(filme));
        }
        return dtos;
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

    // synchronized: duas requisições simultâneas para o mesmo filme não podem importar em dobro
    // (o tmdbId é único). A importação roda e faz commit na sua própria transação.
    public synchronized Long obterOuImportarId(Long tmdbId) {
        boolean jaExistia = movieRepository.existsByTmdbId(tmdbId);
        Long id = tmdbImportService.obterOuImportar(tmdbId).getId();
        if (!jaExistia) {
            graphCache.invalidar(); // filme novo no acervo: o grafo precisa incluí-lo
        }
        return id;
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

        TmdbCreditsDTO creditos = tmdbClient.buscarDetalhesComCreditos(filme.getTmdbId()).getCredits();
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
        MovieCreditsDTO resultado = new MovieCreditsDTO(direcao, elenco);
        cacheCreditos.put(filme.getTmdbId(), new CreditosEmCache(resultado, Instant.now()));
        return resultado;
    }
}
