package com.lucasbarros.nitrate.graph;

import com.lucasbarros.nitrate.entities.Actor;
import com.lucasbarros.nitrate.entities.Director;
import com.lucasbarros.nitrate.entities.Franchise;
import com.lucasbarros.nitrate.entities.Genre;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.MovieCast;
import com.lucasbarros.nitrate.entities.RoleType;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Mantém o grafo em memória. Montar o grafo exige carregar o acervo inteiro (com elenco e
 * gêneros), alguns MB vindos do banco, então isso acontece uma vez e só se repete quando
 * o grafo fica velho demais ou numa importação em lote pelo admin (invalidar()).
 *
 * Filme importado ao abrir a página não invalida: ele entra no grafo na próxima montagem.
 * Remontar a cada filme novo estourava a cota de transferência do banco.
 */
@Component
public class MovieGraphCache {

    private static final Duration VALIDADE = Duration.ofHours(6);

    // mesma régua das sementes da recomendação: curtiu ou deu 3,5 para cima
    private static final double NOTA_MINIMA_CURTIDA = 3.5;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private ReviewRepository reviewRepository;

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
        if (atual != null) {
            // vencido mas existe: um pedido remonta e os outros seguem com o grafo antigo em vez de esperar
            if (!montagem.tryLock()) return atual;
        } else {
            montagem.lock();
        }
        try {
            if (grafo == null || montadoEm.plus(VALIDADE).isBefore(Instant.now())) {
                List<Movie> acervo = carregarAcervo();
                Map<Long, List<Long>> curtidas = carregarCurtidas();
                montadoEm = Instant.now();
                grafo = new MovieGraph(acervo, curtidas);
            }
            return grafo;
        } finally {
            montagem.unlock();
        }
    }

    /**
     * Monta os filmes do grafo a partir das colunas que ele usa. São objetos soltos, fora do
     * Hibernate: sem overview e só com id, nome e papel em diretor, franquia, gêneros e elenco.
     * Diretores, franquias, gêneros e atores repetidos viram a mesma instância.
     */
    private List<Movie> carregarAcervo() {
        Map<Long, Movie> porId = new LinkedHashMap<>();
        Map<Long, Director> diretores = new HashMap<>();
        Map<Long, Franchise> franquias = new HashMap<>();
        Map<Long, Genre> generos = new HashMap<>();
        Map<Long, Actor> atores = new HashMap<>();

        for (Object[] linha : movieRepository.findResumosParaGrafo()) {
            Movie filme = new Movie();
            filme.setId((Long) linha[0]);
            filme.setTitle((String) linha[1]);
            filme.setReleaseYear((Integer) linha[2]);
            filme.setRating((Double) linha[3]);
            filme.setTmdbId((Long) linha[4]);
            filme.setSlug((String) linha[5]);
            filme.setPosterPath((String) linha[6]);
            filme.setReleaseDate((LocalDate) linha[7]);
            if (linha[8] != null) {
                filme.setDirector(diretores.computeIfAbsent((Long) linha[8], id -> comId(new Director((String) linha[9]), id)));
            }
            if (linha[10] != null) {
                filme.setFranchise(franquias.computeIfAbsent((Long) linha[10], id -> comId(new Franchise((String) linha[11]), id)));
            }
            porId.put(filme.getId(), filme);
        }

        for (Object[] linha : movieRepository.findGenerosParaGrafo()) {
            Movie filme = porId.get((Long) linha[0]);
            if (filme == null) continue; // importado entre uma consulta e outra
            filme.getGenres().add(generos.computeIfAbsent((Long) linha[1], id -> comId(new Genre((String) linha[2]), id)));
        }

        for (Object[] linha : movieRepository.findElencoParaGrafo()) {
            Movie filme = porId.get((Long) linha[0]);
            if (filme == null) continue;
            Actor ator = atores.computeIfAbsent((Long) linha[1], id -> comId(new Actor((String) linha[2]), id));
            filme.getCast().add(new MovieCast(filme, ator, (RoleType) linha[3]));
        }

        return new ArrayList<>(porId.values());
    }

    private static Director comId(Director diretor, Long id) { diretor.setId(id); return diretor; }
    private static Franchise comId(Franchise franquia, Long id) { franquia.setId(id); return franquia; }
    private static Genre comId(Genre genero, Long id) { genero.setId(id); return genero; }
    private static Actor comId(Actor ator, Long id) { ator.setId(id); return ator; }

    /** curtidas de todo mundo, para o "quem curtiu também curtiu": só pares de ids */
    private Map<Long, List<Long>> carregarCurtidas() {
        Map<Long, List<Long>> porUsuario = new HashMap<>();
        for (Object[] linha : reviewRepository.findCurtidasParaGrafo(NOTA_MINIMA_CURTIDA)) {
            porUsuario.computeIfAbsent((Long) linha[0], k -> new ArrayList<>()).add((Long) linha[1]);
        }
        return porUsuario;
    }

    /** chamar depois de importar filmes; o próximo obter() remonta o grafo */
    public void invalidar() {
        grafo = null;
    }
}
