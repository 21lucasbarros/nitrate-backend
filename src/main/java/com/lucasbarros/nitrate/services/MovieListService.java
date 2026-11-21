package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.ListEngagementDTO;
import com.lucasbarros.nitrate.dto.MovieListDTO;
import com.lucasbarros.nitrate.dto.MovieListRequestDTO;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.MovieList;
import com.lucasbarros.nitrate.entities.MovieListEntry;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.ListCommentRepository;
import com.lucasbarros.nitrate.repositories.ListLikeRepository;
import com.lucasbarros.nitrate.repositories.ListSaveRepository;
import com.lucasbarros.nitrate.repositories.MovieListRepository;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.services.exceptions.ForbiddenException;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Listas de filmes criadas pelas pessoas. Privadas só aparecem para a dona. Na inicialização,
 * gera o slug das listas que ainda não têm (as criadas antes do endereço /u/{dona}/lista/{slug}).
 */
@Service
public class MovieListService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MovieListService.class);

    public static final int NOME_MAX = 100;
    public static final int DESCRICAO_MAX = 2000;
    public static final int FILMES_MAX = 500;
    private static final int SLUG_MAX = 80;

    @Autowired private MovieListRepository movieListRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ListLikeRepository listLikeRepository;
    @Autowired private ListCommentRepository listCommentRepository;
    @Autowired private ListSaveRepository listSaveRepository;

    /** as listas do perfil: quem vê o próprio perfil vê também as privadas */
    @Transactional(readOnly = true)
    public List<MovieListDTO> listasDoUsuario(String username, User quemVe) {
        User dono = userRepository.findFirstByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + username));
        List<MovieList> listas = ehDono(dono, quemVe)
                ? movieListRepository.findByUserOrderByUpdatedAtDesc(dono)
                : movieListRepository.findByUserAndPublicListTrueOrderByUpdatedAtDesc(dono);
        return resumos(listas, quemVe);
    }

    /** minhas listas; com `movieId`, cada uma diz se já tem o filme (para o "adicionar à lista") */
    @Transactional(readOnly = true)
    public List<MovieListDTO> minhasListas(User eu, Long movieId) {
        return movieListRepository.findByUserOrderByUpdatedAtDesc(eu).stream()
                .map(l -> MovieListDTO.resumo(l, movieId == null ? (Boolean) null : contem(l, movieId)))
                .toList();
    }

    /** pelo id: o front usa só para levar endereços antigos (/lista/{id}) ao novo */
    @Transactional(readOnly = true)
    public MovieListDTO buscar(Long id, User quemVe) {
        MovieList lista = buscarVisivel(id, quemVe);
        return MovieListDTO.completa(lista, engajamento(lista, quemVe));
    }

    /** a página da lista: /u/{username}/lista/{slug} */
    @Transactional(readOnly = true)
    public MovieListDTO buscarPorSlug(String username, String slug, User quemVe) {
        MovieList lista = userRepository.findFirstByUsernameIgnoreCase(username.trim())
                .flatMap(dono -> movieListRepository.findByUserAndSlug(dono, slug))
                // privada de outra pessoa: responde como se não existisse, para não revelar que ela existe
                .filter(l -> l.isPublicList() || ehDono(l.getUser(), quemVe))
                .orElseThrow(() -> new ResourceNotFoundException("Lista não encontrada: " + slug));
        return MovieListDTO.completa(lista, engajamento(lista, quemVe));
    }

    @Transactional
    public MovieListDTO criar(User eu, MovieListRequestDTO dados) {
        MovieList lista = new MovieList(userRepository.findById(eu.getId()).orElseThrow());
        preencher(lista, dados);
        lista = movieListRepository.save(lista);
        definirFilmes(lista, dados.movieIds());
        return MovieListDTO.completa(lista, engajamento(lista, eu));
    }

    /** substitui nome, descrição, visibilidade e os filmes (na ordem recebida) */
    @Transactional
    public MovieListDTO atualizar(User eu, Long id, MovieListRequestDTO dados) {
        MovieList lista = buscarMinha(eu, id);
        preencher(lista, dados);
        definirFilmes(lista, dados.movieIds());
        return MovieListDTO.completa(lista, engajamento(lista, eu));
    }

    @Transactional
    public void excluir(User eu, Long id) {
        MovieList lista = buscarMinha(eu, id);
        limparEngajamento(lista);
        movieListRepository.delete(lista);
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

    /**
     * exclusão da conta (LGPD): as listas da pessoa (as entradas vão junto pelo cascade, curtidas,
     * comentários e salvamentos de outros são apagados antes) e o que ela fez nas listas dos outros
     */
    @Transactional
    public void excluirTodas(User user) {
        List<MovieList> listas = movieListRepository.findByUserOrderByUpdatedAtDesc(user);
        listas.forEach(this::limparEngajamento);
        movieListRepository.deleteAll(listas);
        listLikeRepository.deleteAllByUser(user);
        listCommentRepository.deleteAllByUser(user);
        listSaveRepository.deleteAllByUser(user);
    }

    /** gera o slug das listas antigas, todas de uma vez (uma consulta por lista seria lenta no banco remoto) */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<MovieList> semSlug = movieListRepository.findBySlugIsNull();
        if (semSlug.isEmpty()) return;
        Map<Long, Set<String>> usadosPorDona = new HashMap<>();
        for (MovieList lista : semSlug) {
            Set<String> usados = usadosPorDona.computeIfAbsent(lista.getUser().getId(),
                    id -> new HashSet<>(movieListRepository.slugsDoUsuario(lista.getUser(), -1L)));
            String slug = MovieSlugService.livre(baseDoSlug(lista.getName()), usados::contains);
            usados.add(slug);
            lista.setSlug(slug);
        }
        movieListRepository.saveAll(semSlug);
        log.info("slug gerado para {} lista(s)", semSlug.size());
    }

    // ---------- engajamento: curtidas, comentários, salvamentos ----------

    /** resumos de várias listas, com o engajamento de todas em poucas consultas */
    @Transactional(readOnly = true)
    public List<MovieListDTO> resumos(List<MovieList> listas, User quemVe) {
        if (listas.isEmpty()) return List.of();
        List<Long> ids = listas.stream().map(MovieList::getId).toList();
        Map<Long, Long> curtidas = contagens(listLikeRepository.contarPorLista(ids));
        Map<Long, Long> comentarios = contagens(listCommentRepository.contarPorLista(ids));
        Set<Long> curti = quemVe == null ? Set.of() : new HashSet<>(listLikeRepository.listasMinhas(quemVe, ids));
        Set<Long> salvei = quemVe == null ? Set.of() : new HashSet<>(listSaveRepository.listasMinhas(quemVe, ids));
        return listas.stream()
                .map(l -> MovieListDTO.resumo(l, new ListEngagementDTO(
                        curtidas.getOrDefault(l.getId(), 0L),
                        comentarios.getOrDefault(l.getId(), 0L),
                        quemVe == null ? null : curti.contains(l.getId()),
                        quemVe == null ? null : salvei.contains(l.getId()))))
                .toList();
    }

    public ListEngagementDTO engajamento(MovieList lista, User quemVe) {
        List<Long> id = List.of(lista.getId());
        return new ListEngagementDTO(
                contagens(listLikeRepository.contarPorLista(id)).getOrDefault(lista.getId(), 0L),
                contagens(listCommentRepository.contarPorLista(id)).getOrDefault(lista.getId(), 0L),
                quemVe == null ? null : listLikeRepository.existsByListAndUser(lista, quemVe),
                quemVe == null ? null : listSaveRepository.existsByListAndUser(lista, quemVe));
    }

    /** a lista, se quem vê pode vê-la; privada de outra pessoa responde como inexistente */
    public MovieList buscarVisivel(Long id, User quemVe) {
        MovieList lista = buscarLista(id);
        if (!lista.isPublicList() && !ehDono(lista.getUser(), quemVe)) {
            throw new ResourceNotFoundException("Lista não encontrada. Id: " + id);
        }
        return lista;
    }

    /** a lista vai ser apagada: curtidas, comentários e salvamentos vão junto */
    private void limparEngajamento(MovieList lista) {
        listLikeRepository.deleteAllByList(lista);
        listCommentRepository.deleteAllByList(lista);
        listSaveRepository.deleteAllByList(lista);
    }

    private static Map<Long, Long> contagens(List<Object[]> linhas) {
        return linhas.stream().collect(Collectors.toMap(l -> (Long) l[0], l -> (Long) l[1]));
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

        boolean renomeada = !nome.equals(lista.getName());
        lista.setName(nome);
        // o endereço acompanha o nome (como no Letterboxd)
        if (renomeada || lista.getSlug() == null) {
            Long id = lista.getId() == null ? -1L : lista.getId();
            Set<String> usados = new HashSet<>(movieListRepository.slugsDoUsuario(lista.getUser(), id));
            lista.setSlug(MovieSlugService.livre(baseDoSlug(nome), usados::contains));
        }
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
        // quem continua na lista mantém a data em que entrou (a atividade do filme usa)
        Map<Long, Instant> entradas = new HashMap<>();
        lista.getEntries().forEach(e -> {
            if (e.getAddedAt() != null) entradas.put(e.getMovie().getId(), e.getAddedAt());
        });
        // o Hibernate insere antes de apagar: sem o flush, um filme que continua na lista
        // esbarraria na restrição única (lista, filme)
        lista.getEntries().clear();
        movieListRepository.saveAndFlush(lista);
        for (int i = 0; i < ids.size(); i++) {
            MovieListEntry entrada = new MovieListEntry(lista, buscarFilme(ids.get(i)), i + 1);
            if (entradas.containsKey(ids.get(i))) entrada.setAddedAt(entradas.get(ids.get(i)));
            lista.getEntries().add(entrada);
        }
    }

    private static String baseDoSlug(String nome) {
        String slug = MovieSlugService.slugify(nome, SLUG_MAX);
        // nome sem letras latinas (ex.: só emoji): "lista", "lista-2"...
        return slug.isEmpty() ? "lista" : slug;
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
