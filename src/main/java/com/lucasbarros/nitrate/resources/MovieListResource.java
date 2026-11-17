package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.MovieListDTO;
import com.lucasbarros.nitrate.dto.MovieListRequestDTO;
import com.lucasbarros.nitrate.security.CustomUserDetails;
import com.lucasbarros.nitrate.services.MovieListService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lists")
public class MovieListResource {

    @Autowired
    private MovieListService movieListService;

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
}
