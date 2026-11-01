package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.MovieDTO;
import com.lucasbarros.nitrate.dto.MovieSearchResultDTO;
import com.lucasbarros.nitrate.graph.MovieGraphCache;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import com.lucasbarros.nitrate.tmdb.TmdbClient;
import com.lucasbarros.nitrate.tmdb.TmdbImportService;
import com.lucasbarros.nitrate.tmdb.dto.TmdbMovieSummaryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<Long, Long> idLocalPorTmdbId = new HashMap<>();
        for (Movie filme : movieRepository.findByTmdbIdIn(tmdbIds)) {
            idLocalPorTmdbId.put(filme.getTmdbId(), filme.getId());
        }

        for (TmdbMovieSummaryDTO resumo : encontrados) {
            resultados.add(new MovieSearchResultDTO(resumo, idLocalPorTmdbId.get(resumo.getId())));
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
}
