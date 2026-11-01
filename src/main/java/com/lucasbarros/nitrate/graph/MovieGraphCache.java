package com.lucasbarros.nitrate.graph;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Mantém o grafo em memória. Montar o grafo exige carregar o acervo inteiro (com elenco e
 * gêneros), então isso acontece uma vez e só se repete quando o acervo muda (invalidar()).
 */
@Component
public class MovieGraphCache {

    @Autowired
    private MovieRepository movieRepository;

    private volatile MovieGraph grafo;

    @Transactional(readOnly = true)
    public synchronized MovieGraph obter() {
        if (grafo == null) {
            // a consulta já traz elenco/gêneros carregados, então o grafo continua
            // utilizável depois que a transação termina
            List<Movie> acervo = movieRepository.findAllParaGrafo();
            // mesma transação: o elenco é preenchido nas mesmas instâncias de `acervo`
            movieRepository.carregarElenco(acervo);
            grafo = new MovieGraph(acervo);
        }
        return grafo;
    }

    /** chamar depois de importar filmes; o próximo obter() remonta o grafo */
    public void invalidar() {
        grafo = null;
    }
}
