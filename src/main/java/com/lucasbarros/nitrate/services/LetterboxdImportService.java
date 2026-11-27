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

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

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
                // as mensagens de validação são nossas; o texto de outras exceções pode ter detalhes internos
                String detalhe = e instanceof IllegalArgumentException ? e.getMessage() : "Não foi possível importar este filme.";
                resultados.add(new ItemResult(item.name(), item.year(), Status.ERROR, null, null, detalhe));
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

        // data no futuro deixaria a review no topo das recentes e dos destaques até lá: vira hoje
        LocalDate registradoEm = item.loggedOn() != null && item.loggedOn().isAfter(LocalDate.now(ZoneOffset.UTC))
                ? null : item.loggedOn();

        Status status;
        if (item.watchlist()) {
            status = watchlistService.adicionarSeNaoAssistiu(user, movieId, registradoEm);
        } else {
            ReviewRequestDTO request = new ReviewRequestDTO();
            request.setRating(item.rating());
            request.setLiked(item.liked());
            request.setWatchedOn(item.watchedOn() != null ? item.watchedOn() : registradoEm);
            request.setText(item.review());
            boolean criada = reviewService.criarSeNaoExistir(user, movieId, request, registradoEm);
            status = criada ? Status.CREATED : Status.ALREADY_EXISTS;
        }
        return new ItemResult(item.name(), item.year(), status, movieId, encontrado.getTitle(), null);
    }

    /**
     * Primeiro procura filmes com alguma estreia no ano do Letterboxd (que costuma usar a
     * primeira exibição, mesmo em festival). Sem resultado, aceita 1 ano de diferença.
     */
    private TmdbMovieSummaryDTO encontrarNoTmdb(String nome, Integer ano) {
        if (ano != null) {
            TmdbMovieSummaryDTO noAno = melhorCandidato(nome, tmdbClient.pesquisarFilmes(nome, ano).getResults());
            if (noAno != null) return noAno;
        }
        List<TmdbMovieSummaryDTO> proximos = tmdbClient.pesquisarFilmes(nome).getResults().stream()
                .filter(r -> {
                    Integer anoResultado = anoDe(r.getReleaseDate());
                    return ano == null || (anoResultado != null && Math.abs(anoResultado - ano) <= 1);
                })
                .toList();
        return melhorCandidato(nome, proximos);
    }

    /**
     * Títulos repetidos são comuns ("Obsession" tem dezenas). O primeiro resultado do TMDB
     * pode ser um curta obscuro, então preferimos quem tem exatamente o título procurado
     * (traduzido ou original) e, entre esses, o mais votado. Empate fica com a ordem do TMDB.
     */
    static TmdbMovieSummaryDTO melhorCandidato(String nome, List<TmdbMovieSummaryDTO> candidatos) {
        String procurado = normalizar(nome);
        return candidatos.stream()
                .max(Comparator
                        .comparing((TmdbMovieSummaryDTO r) ->
                                procurado.equals(normalizar(r.getTitle())) || procurado.equals(normalizar(r.getOriginalTitle())))
                        .thenComparingInt(TmdbMovieSummaryDTO::getVoteCount)
                        // max() fica com o último empatado; o índice negativo mantém o primeiro do TMDB
                        .thenComparingInt(r -> -candidatos.indexOf(r)))
                .orElse(null);
    }

    /** "Amélie!" e "amelie" contam como o mesmo título */
    private static String normalizar(String titulo) {
        if (titulo == null) return "";
        return Normalizer.normalize(titulo, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]", "");
    }

    private Integer anoDe(String data) {
        return data != null && data.length() >= 4 ? Integer.valueOf(data.substring(0, 4)) : null;
    }
}
