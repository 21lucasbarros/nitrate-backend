package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.MovieListDTO;
import com.lucasbarros.nitrate.dto.MovieListRequestDTO;
import com.lucasbarros.nitrate.services.MovieListService;
import com.lucasbarros.nitrate.services.NitrateAccountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Listas do Nitrate: o admin cria, edita e apaga listas como a conta de sistema "nitrate", com as
 * mesmas regras das listas de todo mundo. O acesso é conferido no SecurityConfig (/admin/**).
 */
@RestController
@RequestMapping("/admin/lists")
public class AdminListResource {

    @Autowired
    private MovieListService movieListService;

    @Autowired
    private NitrateAccountService nitrateAccountService;

    // todas, inclusive as privadas (rascunhos), da atualizada mais recentemente para a mais antiga
    @GetMapping
    public List<MovieListDTO> listas() {
        return movieListService.listasDoUsuario(NitrateAccountService.USERNAME, nitrateAccountService.conta());
    }

    // completa, para o editor; os rascunhos não abrem pela rota pública (só a dona vê)
    @GetMapping("/{id}")
    public MovieListDTO buscar(@PathVariable Long id) {
        return movieListService.buscar(id, nitrateAccountService.conta());
    }

    @PostMapping
    public MovieListDTO criar(@RequestBody MovieListRequestDTO request) {
        return movieListService.criar(nitrateAccountService.conta(), request);
    }

    @PutMapping("/{id}")
    public MovieListDTO atualizar(@PathVariable Long id, @RequestBody MovieListRequestDTO request) {
        return movieListService.atualizar(nitrateAccountService.conta(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        movieListService.excluir(nitrateAccountService.conta(), id);
        return ResponseEntity.noContent().build();
    }
}
