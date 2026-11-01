package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.tmdb.TmdbClient;
import com.lucasbarros.nitrate.tmdb.dto.TmdbCastMemberDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbCrewMemberDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbMovieDetailDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Faz o grafo crescer a partir do que os usuários gostam: quando alguém curte (ou avalia bem)
 * um filme, importamos os filmes mais conhecidos do diretor e do elenco principal dele.
 * É isso que cria conexões de "mesmo diretor" e "elenco em comum" — sem esses filmes no
 * acervo, o grafo só consegue ligar por gênero.
 */
@Service
public class GraphExpansionService {

    public static final double NOTA_MINIMA_SEMENTE = 3.5;

    private static final Logger log = LoggerFactory.getLogger(GraphExpansionService.class);

    private static final int FILMES_POR_DIRETOR = 6;
    private static final int ATORES_POR_FILME = 3;
    private static final int FILMES_POR_ATOR = 4;
    private static final int VOTOS_MINIMOS = 200; // evita filmes obscuros que ninguém reconhece

    @Autowired private MovieRepository movieRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private TmdbClient tmdbClient;
    @Autowired private PersonService personService;
    @Autowired private MovieService movieService;

    @Async
    public void expandir(Long movieId) {
        try {
            expandirAgora(movieId);
        } catch (Exception e) {
            log.warn("Falha ao expandir o grafo a partir do filme {}: {}", movieId, e.getMessage());
        }
    }

    /** expande a partir de todos os filmes que já foram curtidos/bem avaliados (backfill) */
    @Async
    public void expandirTodasAsSementes() {
        List<Long> sementes = reviewRepository.findIdsDeFilmesSemente(NOTA_MINIMA_SEMENTE);
        log.info("Expandindo o grafo a partir de {} filme(s) semente", sementes.size());
        for (Long movieId : sementes) {
            expandir(movieId); // chamada interna: roda nesta mesma thread, um filme por vez
        }
        log.info("Expansão do grafo concluída");
    }

    /** @return quantos filmes novos entraram no acervo */
    public int expandirAgora(Long movieId) {
        Movie filme = movieRepository.findById(movieId).orElse(null);
        if (filme == null || filme.isGrafoExpandido() || filme.getTmdbId() == null) {
            return 0;
        }

        TmdbMovieDetailDTO detalhe = tmdbClient.buscarDetalhesComCreditos(filme.getTmdbId());
        Set<Long> paraImportar = new LinkedHashSet<>();

        if (detalhe.getCredits() != null) {
            for (TmdbCrewMemberDTO membro : detalhe.getCredits().getCrew()) {
                if ("Director".equals(membro.getJob()) && membro.getId() != null) {
                    paraImportar.addAll(personService.destaques(membro.getId(), true, FILMES_POR_DIRETOR, VOTOS_MINIMOS));
                }
            }

            List<TmdbCastMemberDTO> elenco = new ArrayList<>(detalhe.getCredits().getCast());
            elenco.sort(Comparator.comparing(m -> m.getOrder() == null ? Integer.MAX_VALUE : m.getOrder()));
            int atores = 0;
            for (TmdbCastMemberDTO membro : elenco) {
                if (membro.getId() == null) continue;
                paraImportar.addAll(personService.destaques(membro.getId(), false, FILMES_POR_ATOR, VOTOS_MINIMOS));
                if (++atores == ATORES_POR_FILME) break;
            }
        }
        paraImportar.remove(filme.getTmdbId());

        int novos = 0;
        for (Long tmdbId : paraImportar) {
            if (movieRepository.existsByTmdbId(tmdbId)) continue;
            try {
                movieService.obterOuImportarId(tmdbId); // serializado e invalida o cache do grafo
                novos++;
            } catch (Exception e) {
                // um filme com problema no TMDB não deve impedir a expansão dos outros
                log.warn("Não foi possível importar o filme TMDB {}: {}", tmdbId, e.getMessage());
            }
        }
        movieRepository.marcarGrafoExpandido(movieId);
        log.info("Grafo expandido a partir de '{}': {} filme(s) novo(s)", filme.getTitle(), novos);
        return novos;
    }
}
