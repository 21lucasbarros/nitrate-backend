package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.ListCommentDTO;
import com.lucasbarros.nitrate.dto.ListEngagementDTO;
import com.lucasbarros.nitrate.dto.MovieListDTO;
import com.lucasbarros.nitrate.dto.MovieListRequestDTO;
import com.lucasbarros.nitrate.dto.ReviewCommentRequestDTO;
import com.lucasbarros.nitrate.security.CustomUserDetails;
import com.lucasbarros.nitrate.services.ListSocialService;
import com.lucasbarros.nitrate.services.MovieListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/lists")
public class MovieListResource {

    @Autowired
    private MovieListService movieListService;

    @Autowired
    private ListSocialService listSocialService;

    // rota pública: as listas públicas mais curtidas (home). Antes de /{id} por clareza; "popular" não é número
    @GetMapping("/popular")
    public List<MovieListDTO> populares(@AuthenticationPrincipal CustomUserDetails principal,
                                        @RequestParam(defaultValue = "6") int limit) {
        return listSocialService.populares(limit, principal == null ? null : principal.getUser());
    }

    // rota pública: listas privadas só aparecem para quem as criou
    @GetMapping("/{id}")
    public MovieListDTO buscar(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        return movieListService.buscar(id, principal == null ? null : principal.getUser());
    }

    @PostMapping
    public MovieListDTO criar(@AuthenticationPrincipal CustomUserDetails principal,
                              @RequestBody MovieListRequestDTO request) {
        return movieListService.criar(principal.getUser(), request);
    }

    @PutMapping("/{id}")
    public MovieListDTO atualizar(@AuthenticationPrincipal CustomUserDetails principal,
                                  @PathVariable Long id,
                                  @RequestBody MovieListRequestDTO request) {
        return movieListService.atualizar(principal.getUser(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        movieListService.excluir(principal.getUser(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/movies/{movieId}")
    public MovieListDTO adicionarFilme(@AuthenticationPrincipal CustomUserDetails principal,
                                       @PathVariable Long id,
                                       @PathVariable Long movieId) {
        return movieListService.adicionarFilme(principal.getUser(), id, movieId);
    }

    @DeleteMapping("/{id}/movies/{movieId}")
    public MovieListDTO removerFilme(@AuthenticationPrincipal CustomUserDetails principal,
                                     @PathVariable Long id,
                                     @PathVariable Long movieId) {
        return movieListService.removerFilme(principal.getUser(), id, movieId);
    }

    @PostMapping("/{id}/like")
    public ListEngagementDTO curtir(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        return listSocialService.curtir(principal.getUser(), id);
    }

    @DeleteMapping("/{id}/like")
    public ListEngagementDTO descurtir(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        return listSocialService.descurtir(principal.getUser(), id);
    }

    @PostMapping("/{id}/save")
    public ListEngagementDTO salvar(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        return listSocialService.salvar(principal.getUser(), id);
    }

    @DeleteMapping("/{id}/save")
    public ListEngagementDTO deixarDeSalvar(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        return listSocialService.deixarDeSalvar(principal.getUser(), id);
    }

    // rota pública; comentários de lista privada só para a dona
    @GetMapping("/{id}/comments")
    public List<ListCommentDTO> comentarios(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        return listSocialService.comentarios(id, principal == null ? null : principal.getUser());
    }

    // o corpo é o mesmo do comentário de review: { "text": "..." }
    @PostMapping("/{id}/comments")
    public ResponseEntity<ListCommentDTO> comentar(@AuthenticationPrincipal CustomUserDetails principal,
                                                   @PathVariable Long id,
                                                   @RequestBody ReviewCommentRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(listSocialService.comentar(principal.getUser(), id, request.text()));
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> excluirComentario(@AuthenticationPrincipal CustomUserDetails principal,
                                                  @PathVariable Long id,
                                                  @PathVariable Long commentId) {
        listSocialService.excluirComentario(principal.getUser(), id, commentId);
        return ResponseEntity.noContent().build();
    }
}
