package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.MovieListDTO;
import com.lucasbarros.nitrate.dto.MovieListRequestDTO;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.MovieList;
import com.lucasbarros.nitrate.entities.MovieListEntry;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.MovieListRepository;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.services.exceptions.ForbiddenException;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** Listas de filmes criadas pelas pessoas. Privadas só aparecem para a dona. */
@Service
public class MovieListService {

    public static final int NOME_MAX = 100;
    public static final int DESCRICAO_MAX = 2000;
    public static final int FILMES_MAX = 500;

    @Autowired private MovieListRepository movieListRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private UserRepository userRepository;

    /** as listas do perfil: quem vê o próprio perfil vê também as privadas */
    @Transactional(readOnly = true)
    public List<MovieListDTO> listasDoUsuario(String username, User quemVe) {
        User dono = userRepository.findByUsername(username.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + username));
        List<MovieList> listas = ehDono(dono, quemVe)
                ? movieListRepository.findByUserOrderByUpdatedAtDesc(dono)
                : movieListRepository.findByUserAndPublicListTrueOrderByUpdatedAtDesc(dono);
        return listas.stream().map(l -> MovieListDTO.resumo(l, null)).toList();
    }

    /** minhas listas; com `movieId`, cada uma diz se já tem o filme (para o "adicionar à lista") */
    @Transactional(readOnly = true)
    public List<MovieListDTO> minhasListas(User eu, Long movieId) {
        return movieListRepository.findByUserOrderByUpdatedAtDesc(eu).stream()
                .map(l -> MovieListDTO.resumo(l, movieId == null ? null : contem(l, movieId)))
                .toList();
    }

    @Transactional(readOnly = true)
    public MovieListDTO buscar(Long id, User quemVe) {
        MovieList lista = buscarLista(id);
        // privada de outra pessoa: responde como se não existisse, para não revelar que ela existe
        if (!lista.isPublicList() && !ehDono(lista.getUser(), quemVe)) {
            throw new ResourceNotFoundException("Lista não encontrada. Id: " + id);
        }
        return MovieListDTO.completa(lista);
    }

    @Transactional
    public MovieListDTO criar(User eu, MovieListRequestDTO dados) {
        MovieList lista = new MovieList(userRepository.findById(eu.getId()).orElseThrow());
        preencher(lista, dados);
        lista = movieListRepository.save(lista);
        definirFilmes(lista, dados.movieIds());
        return MovieListDTO.completa(lista);
    }

    /** substitui nome, descrição, visibilidade e os filmes (na ordem recebida) */
    @Transactional
    public MovieListDTO atualizar(User eu, Long id, MovieListRequestDTO dados) {
        MovieList lista = buscarMinha(eu, id);
        preencher(lista, dados);
        definirFilmes(lista, dados.movieIds());
        return MovieListDTO.completa(lista);
    }

    @Transactional
    public void excluir(User eu, Long id) {
        movieListRepository.delete(buscarMinha(eu, id));
    }

    /** idempotente: o filme vai para o fim da lista, se ainda não estiver nela */
    @Transactional
    public MovieListDTO adicionarFilme(User eu, Long id, Long movieId) {
        MovieList lista = buscarMinha(eu, id);
        if (!contem(lista, movieId)) {
            if (lista.getEntries().size() >= FILMES_MAX) {
                throw new IllegalArgumentException("Uma lista pode ter até " + FILMES_MAX + " filmes.");
            }
            int ultima = lista.getEntries().stream().mapToInt(MovieListEntry::getPosition).max().orElse(0);
            lista.getEntries().add(new MovieListEntry(lista, buscarFilme(movieId), ultima + 1));
            lista.setUpdatedAt(Instant.now());
        }
        return MovieListDTO.resumo(lista, true);
    }

    @Transactional
    public MovieListDTO removerFilme(User eu, Long id, Long movieId) {
        MovieList lista = buscarMinha(eu, id);
        if (lista.getEntries().removeIf(e -> e.getMovie().getId().equals(movieId))) {
            lista.setUpdatedAt(Instant.now());
        }
        return MovieListDTO.resumo(lista, false);
    }

    /** exclusão da conta (LGPD): as entradas vão junto pelo cascade */
    @Transactional
    public void excluirTodas(User user) {
        movieListRepository.deleteAll(movieListRepository.findByUserOrderByUpdatedAtDesc(user));
    }

    private void preencher(MovieList lista, MovieListRequestDTO dados) {
        String nome = dados.name() == null ? "" : dados.name().trim();
        if (nome.isEmpty()) throw new IllegalArgumentException("Dê um nome para a lista.");
        if (nome.length() > NOME_MAX) throw new IllegalArgumentException("O nome pode ter até " + NOME_MAX + " caracteres.");

        String descricao = dados.description() == null ? null : dados.description().trim();
        if (descricao != null && descricao.isEmpty()) descricao = null;
        if (descricao != null && descricao.length() > DESCRICAO_MAX) {
            throw new IllegalArgumentException("A descrição pode ter até " + DESCRICAO_MAX + " caracteres.");
        }

        lista.setName(nome);
        lista.setDescription(descricao);
        lista.setPublicList(dados.isPublic() == null || dados.isPublic());
        lista.setUpdatedAt(Instant.now());
    }

    private void definirFilmes(MovieList lista, List<Long> movieIds) {
        // repetidos contam uma vez só
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(movieIds == null ? List.of() : movieIds));
        if (ids.size() > FILMES_MAX) {
            throw new IllegalArgumentException("Uma lista pode ter até " + FILMES_MAX + " filmes.");
        }
        // o Hibernate insere antes de apagar: sem o flush, um filme que continua na lista
        // esbarraria na restrição única (lista, filme)
        lista.getEntries().clear();
        movieListRepository.saveAndFlush(lista);
        for (int i = 0; i < ids.size(); i++) {
            lista.getEntries().add(new MovieListEntry(lista, buscarFilme(ids.get(i)), i + 1));
        }
    }

    private static boolean contem(MovieList lista, Long movieId) {
        return lista.getEntries().stream().anyMatch(e -> e.getMovie().getId().equals(movieId));
    }

    private static boolean ehDono(User dono, User quemVe) {
        return quemVe != null && dono.getId().equals(quemVe.getId());
    }

    private MovieList buscarMinha(User eu, Long id) {
        MovieList lista = buscarLista(id);
        if (!ehDono(lista.getUser(), eu)) {
            throw new ForbiddenException("Só quem criou a lista pode alterá-la.");
        }
        return lista;
    }

    private MovieList buscarLista(Long id) {
        return movieListRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lista não encontrada. Id: " + id));
    }

    private Movie buscarFilme(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Filme não encontrado. Id: " + movieId));
    }
}
