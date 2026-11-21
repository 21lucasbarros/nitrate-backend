package com.lucasbarros.nitrate.taste;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.tmdb.TmdbClient;
import com.lucasbarros.nitrate.tmdb.dto.TmdbMovieSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Os filmes mais populares de cada palavra-chave, buscados no TMDB e guardados por um dia.
 * Não precisa guardar as palavras-chave de cada filme do acervo: basta saber quais tmdbIds são
 * musicais e cruzar. Se o TMDB falhar, fica com a última lista boa (ou nenhuma, e o filtro usa o
 * plano B dele).
 */
@Component
public class TmdbPalavrasChave implements PalavrasChave {

    private static final Logger log = LoggerFactory.getLogger(TmdbPalavrasChave.class);

    // 15 páginas de 20 = os 300 mais populares; o acervo tem os filmes que as pessoas registram,
    // então os musicais dele estão entre os conhecidos
    private static final int PAGINAS = 15;
    private static final Duration VALIDADE = Duration.ofDays(1);
    // depois de uma falha, espera um pouco antes de tentar de novo (cada tentativa são 15 chamadas)
    private static final Duration ESPERA_APOS_FALHA = Duration.ofMinutes(10);

    private record Lista(Set<Long> tmdbIds, Instant em) {
    }

    @Autowired
    private TmdbClient tmdbClient;

    private final Map<Integer, Lista> listas = new ConcurrentHashMap<>();

    /** busca os musicais logo que a aplicação sobe, para a primeira grade do dia não esperar o TMDB */
    @EventListener(ApplicationReadyEvent.class)
    public void aquecer() {
        Thread.startVirtualThread(() -> lista(MUSICAL));
    }

    @Override
    public boolean tem(Movie filme, int palavraChave) {
        return filme.getTmdbId() != null && lista(palavraChave).tmdbIds().contains(filme.getTmdbId());
    }

    @Override
    public boolean disponivel(int palavraChave) {
        return !lista(palavraChave).tmdbIds().isEmpty();
    }

    private Lista lista(int palavraChave) {
        Lista atual = listas.get(palavraChave);
        Instant agora = Instant.now();
        boolean valida = atual != null && atual.em().plus(atual.tmdbIds().isEmpty() ? ESPERA_APOS_FALHA : VALIDADE)
                .isAfter(agora);
        if (valida) return atual;
        // a primeira requisição do dia busca; as que chegarem juntas esperam a mesma busca
        synchronized (this) {
            atual = listas.get(palavraChave);
            valida = atual != null && atual.em().plus(atual.tmdbIds().isEmpty() ? ESPERA_APOS_FALHA : VALIDADE)
                    .isAfter(agora);
            if (valida) return atual;
            Lista nova = buscar(palavraChave, atual);
            listas.put(palavraChave, nova);
            return nova;
        }
    }

    private Lista buscar(int palavraChave, Lista anterior) {
        Set<Long> ids = new HashSet<>();
        try {
            for (int pagina = 1; pagina <= PAGINAS; pagina++) {
                var resposta = tmdbClient.descobrirPorPalavraChave(palavraChave, pagina);
                if (resposta == null || resposta.getResults().isEmpty()) break;
                resposta.getResults().stream().map(TmdbMovieSummaryDTO::getId).filter(Objects::nonNull).forEach(ids::add);
            }
            log.info("palavra-chave {} do TMDB: {} filmes", palavraChave, ids.size());
            return new Lista(Set.copyOf(ids), Instant.now());
        } catch (RuntimeException e) {
            log.warn("não foi possível buscar a palavra-chave {} no TMDB: {}", palavraChave, e.getMessage());
            // mantém a última lista boa por mais um tempo, em vez de ficar sem nada
            if (anterior != null && !anterior.tmdbIds().isEmpty()) {
                return new Lista(anterior.tmdbIds(), Instant.now().minus(VALIDADE).plus(ESPERA_APOS_FALHA));
            }
            return new Lista(Set.of(), Instant.now());
        }
    }
}
