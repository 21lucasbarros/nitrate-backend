package com.lucasbarros.nitrate.graph;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

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

    @Transactional(readOnly = true)
    public synchronized MovieGraph obter() {
        if (grafo == null || montadoEm.plus(VALIDADE).isBefore(Instant.now())) {
            List<Movie> acervo = movieRepository.findAllParaGrafo();
            // mesma transação: o elenco é preenchido nas mesmas instâncias de `acervo`
            movieRepository.carregarElenco(acervo);
            grafo = new MovieGraph(acervo);
            montadoEm = Instant.now();
        }
        return grafo;
    }

    /** chamar depois de importar filmes; o próximo obter() remonta o grafo */
    public void invalidar() {
        grafo = null;
    }
}
