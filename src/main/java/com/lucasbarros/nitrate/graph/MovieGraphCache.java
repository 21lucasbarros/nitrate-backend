package com.lucasbarros.nitrate.graph;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Mantém o grafo em memória. Montar o grafo exige carregar o acervo inteiro (com elenco e
 * gêneros), então isso acontece uma vez e só se repete quando o acervo muda (invalidar())
 * ou quando o grafo fica velho demais.
 *
 * A validade existe porque em produção podem rodar várias instâncias da API: invalidar()
 * só afeta a instância que importou o filme, e as outras se atualizam ao expirar.
 */
@Component
public class MovieGraphCache {

    private static final Duration VALIDADE = Duration.ofMinutes(10);

    @Autowired
    private MovieRepository movieRepository;

    private volatile MovieGraph grafo;
    private volatile Instant montadoEm;

    // ReentrantLock e não synchronized: montar o grafo espera o banco, e um synchronized
    // bloqueado prenderia a thread do sistema com virtual threads ligadas
    private final ReentrantLock montagem = new ReentrantLock();

    @Transactional(readOnly = true)
    public MovieGraph obter() {
        MovieGraph atual = grafo;
        if (atual != null && montadoEm.plus(VALIDADE).isAfter(Instant.now())) {
            return atual; // caminho comum: o grafo está pronto, ninguém espera a trava
        }
        montagem.lock();
        try {
            if (grafo == null || montadoEm.plus(VALIDADE).isBefore(Instant.now())) {
                List<Movie> acervo = movieRepository.findAllParaGrafo();
                // mesma transação: o elenco é preenchido nas mesmas instâncias de `acervo`
                movieRepository.carregarElenco(acervo);
                montadoEm = Instant.now();
                grafo = new MovieGraph(acervo);
            }
            return grafo;
        } finally {
            montagem.unlock();
        }
    }

    /** chamar depois de importar filmes; o próximo obter() remonta o grafo */
    public void invalidar() {
        grafo = null;
    }
}
