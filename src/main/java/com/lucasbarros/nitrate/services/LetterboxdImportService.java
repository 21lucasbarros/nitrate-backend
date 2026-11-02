package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.LetterboxdImportDTO.Item;
import com.lucasbarros.nitrate.dto.LetterboxdImportDTO.ItemResult;
import com.lucasbarros.nitrate.dto.LetterboxdImportDTO.Status;
import com.lucasbarros.nitrate.dto.ReviewRequestDTO;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.tmdb.TmdbClient;
import com.lucasbarros.nitrate.tmdb.dto.TmdbMovieSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Importa um lote de filmes do export do Letterboxd. O export não traz o id do TMDB, então
 * cada filme é encontrado pelo título + ano. É idempotente: reimportar o mesmo arquivo não
 * duplica nada nem sobrescreve o que a pessoa já fez no Nitrate.
 */
@Service
public class LetterboxdImportService {

    public static final int MAX_LOTE = 25; // cada lote precisa caber numa requisição curta

    private static final Logger log = LoggerFactory.getLogger(LetterboxdImportService.class);

    @Autowired private TmdbClient tmdbClient;
    @Autowired private MovieService movieService;
    @Autowired private ReviewService reviewService;
    @Autowired private WatchlistService watchlistService;

    public List<ItemResult> importarLote(User user, List<Item> itens) {
        if (itens == null || itens.isEmpty()) return List.of();
        if (itens.size() > MAX_LOTE) {
            throw new IllegalArgumentException("Envie no máximo " + MAX_LOTE + " filmes por lote");
        }

        List<ItemResult> resultados = new ArrayList<>();
        for (Item item : itens) {
            try {
                resultados.add(importar(user, item));
            } catch (Exception e) {
                // um filme com problema não derruba o lote
                log.warn("Falha ao importar '{}' ({}): {}", item.name(), item.year(), e.getMessage());
                resultados.add(new ItemResult(item.name(), item.year(), Status.ERROR, null, null, e.getMessage()));
            }
        }
        return resultados;
    }

    private ItemResult importar(User user, Item item) {
        if (item.name() == null || item.name().isBlank()) {
            return new ItemResult(item.name(), item.year(), Status.ERROR, null, null, "Filme sem título");
        }

        TmdbMovieSummaryDTO encontrado = encontrarNoTmdb(item.name().trim(), item.year());
        if (encontrado == null) {
            return new ItemResult(item.name(), item.year(), Status.NOT_FOUND, null, null, null);
        }
        Long movieId = movieService.obterOuImportarId(encontrado.getId());

        Status status;
        if (item.watchlist()) {
            status = watchlistService.adicionarSeNaoAssistiu(user, movieId, item.loggedOn());
        } else {
            ReviewRequestDTO request = new ReviewRequestDTO();
            request.setRating(item.rating());
            request.setLiked(item.liked());
            request.setWatchedOn(item.watchedOn() != null ? item.watchedOn() : item.loggedOn());
            request.setText(item.review());
            boolean criada = reviewService.criarSeNaoExistir(user, movieId, request, item.loggedOn());
            status = criada ? Status.CREATED : Status.ALREADY_EXISTS;
        }
        return new ItemResult(item.name(), item.year(), status, movieId, encontrado.getTitle(), null);
    }

    /**
     * Primeiro procura no ano exato. A data de lançamento varia por país (festival vs.
     * estreia), então sem resultado aceitamos uma diferença de 1 ano.
     */
    private TmdbMovieSummaryDTO encontrarNoTmdb(String nome, Integer ano) {
        if (ano != null) {
            List<TmdbMovieSummaryDTO> noAno = tmdbClient.pesquisarFilmes(nome, ano).getResults();
            if (!noAno.isEmpty()) return noAno.get(0);
        }
        for (TmdbMovieSummaryDTO resultado : tmdbClient.pesquisarFilmes(nome).getResults()) {
            Integer anoResultado = anoDe(resultado.getReleaseDate());
            if (ano == null || (anoResultado != null && Math.abs(anoResultado - ano) <= 1)) {
                return resultado;
            }
        }
        return null;
    }

    private Integer anoDe(String data) {
        return data != null && data.length() >= 4 ? Integer.valueOf(data.substring(0, 4)) : null;
    }
}
