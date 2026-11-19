package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.ListCommentDTO;
import com.lucasbarros.nitrate.dto.ListEngagementDTO;
import com.lucasbarros.nitrate.dto.MovieListDTO;
import com.lucasbarros.nitrate.entities.ListComment;
import com.lucasbarros.nitrate.entities.ListLike;
import com.lucasbarros.nitrate.entities.ListSave;
import com.lucasbarros.nitrate.entities.MovieList;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.ListCommentRepository;
import com.lucasbarros.nitrate.repositories.ListLikeRepository;
import com.lucasbarros.nitrate.repositories.ListSaveRepository;
import com.lucasbarros.nitrate.repositories.MovieListRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.services.exceptions.ForbiddenException;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Curtir, comentar e salvar a lista de outra pessoa, e as listas mais curtidas da comunidade. */
@Service
public class ListSocialService {

    public static final int COMENTARIO_MAX = 1000;
    private static final int POPULARES_MAX = 12;

    @Autowired private MovieListService movieListService;
    @Autowired private MovieListRepository movieListRepository;
    @Autowired private ListLikeRepository listLikeRepository;
    @Autowired private ListCommentRepository listCommentRepository;
    @Autowired private ListSaveRepository listSaveRepository;
    @Autowired private UserRepository userRepository;

    // idempotente: curtir de novo não duplica
    @Transactional
    public ListEngagementDTO curtir(User eu, Long listId) {
        MovieList lista = deOutraPessoa(eu, listId, "curtir");
        if (!listLikeRepository.existsByListAndUser(lista, eu)) {
            listLikeRepository.save(new ListLike(lista, userRepository.getReferenceById(eu.getId())));
        }
        return movieListService.engajamento(lista, eu);
    }

    @Transactional
    public ListEngagementDTO descurtir(User eu, Long listId) {
        MovieList lista = movieListService.buscarVisivel(listId, eu);
        listLikeRepository.findByListAndUser(lista, eu).ifPresent(listLikeRepository::delete);
        return movieListService.engajamento(lista, eu);
    }

    @Transactional
    public ListEngagementDTO salvar(User eu, Long listId) {
        MovieList lista = deOutraPessoa(eu, listId, "salvar");
        if (!listSaveRepository.existsByListAndUser(lista, eu)) {
            listSaveRepository.save(new ListSave(lista, userRepository.getReferenceById(eu.getId())));
        }
        return movieListService.engajamento(lista, eu);
    }

    @Transactional
    public ListEngagementDTO deixarDeSalvar(User eu, Long listId) {
        MovieList lista = movieListService.buscarVisivel(listId, eu);
        listSaveRepository.findByListAndUser(lista, eu).ifPresent(listSaveRepository::delete);
        return movieListService.engajamento(lista, eu);
    }

    /** as listas que salvei, da mais recente; as que viraram privadas somem daqui */
    @Transactional(readOnly = true)
    public List<MovieListDTO> salvas(User eu) {
        List<MovieList> listas = listSaveRepository.findByUserOrderByCreatedAtDescIdDesc(eu).stream()
                .map(ListSave::getList)
                .filter(MovieList::isPublicList)
                .toList();
        return movieListService.resumos(listas, eu);
    }

    /** home: as listas públicas mais curtidas (só as que têm ao menos uma curtida) */
    @Transactional(readOnly = true)
    public List<MovieListDTO> populares(int limite, User quemVe) {
        List<Long> ids = listLikeRepository.maisCurtidas(PageRequest.of(0, Math.min(Math.max(1, limite), POPULARES_MAX)));
        Map<Long, MovieList> porId = movieListRepository.findAllById(ids).stream()
                .collect(Collectors.toMap(MovieList::getId, Function.identity()));
        // findAllById não garante a ordem: volta para a ordem das curtidas
        List<MovieList> listas = ids.stream().map(porId::get).filter(l -> l != null).toList();
        return movieListService.resumos(listas, quemVe);
    }

    @Transactional(readOnly = true)
    public List<ListCommentDTO> comentarios(Long listId, User quemVe) {
        MovieList lista = movieListService.buscarVisivel(listId, quemVe);
        return listCommentRepository.findByListOrderByCreatedAtAscIdAsc(lista).stream()
                .map(ListCommentDTO::new)
                .toList();
    }

    @Transactional
    public ListCommentDTO comentar(User eu, Long listId, String texto) {
        String limpo = texto == null ? "" : texto.trim();
        if (limpo.isEmpty()) throw new IllegalArgumentException("Escreva alguma coisa no comentário.");
        if (limpo.length() > COMENTARIO_MAX) {
            throw new IllegalArgumentException("O comentário pode ter até " + COMENTARIO_MAX + " caracteres.");
        }
        MovieList lista = movieListService.buscarVisivel(listId, eu);
        ListComment comentario = new ListComment(lista, userRepository.getReferenceById(eu.getId()), limpo);
        return new ListCommentDTO(listCommentRepository.save(comentario));
    }

    /** quem escreveu o comentário ou a dona da lista podem apagar */
    @Transactional
    public void excluirComentario(User eu, Long listId, Long comentarioId) {
        ListComment comentario = listCommentRepository.findById(comentarioId)
                .filter(c -> c.getList().getId().equals(listId))
                .orElseThrow(() -> new ResourceNotFoundException("Comentário não encontrado. Id: " + comentarioId));
        boolean autor = comentario.getUser().getId().equals(eu.getId());
        boolean donaDaLista = comentario.getList().getUser().getId().equals(eu.getId());
        if (!autor && !donaDaLista) {
            throw new ForbiddenException("Só quem escreveu o comentário ou a dona da lista podem apagá-lo.");
        }
        listCommentRepository.delete(comentario);
    }

    // curtir e salvar são para listas dos outros
    private MovieList deOutraPessoa(User eu, Long listId, String acao) {
        MovieList lista = movieListService.buscarVisivel(listId, eu);
        if (lista.getUser().getId().equals(eu.getId())) {
            throw new IllegalArgumentException("Você não pode " + acao + " a sua própria lista.");
        }
        return lista;
    }
}
