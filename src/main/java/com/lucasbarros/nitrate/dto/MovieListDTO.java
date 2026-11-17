package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.MovieList;
import com.lucasbarros.nitrate.entities.MovieListEntry;

import java.time.Instant;
import java.util.List;

/**
 * Uma lista de filmes. Nas listagens (perfil, "adicionar à lista") `movies` traz só os primeiros
 * pôsteres, para a prévia; na página da lista, todos os filmes em ordem.
 * `containsMovie` só vem preenchido em /users/me/lists?movieId=.
 */
public record MovieListDTO(
        Long id,
        String name,
        String description,
        boolean isPublic,
        int filmsCount,
        List<MovieDTO> movies,
        UserSummaryDTO owner,
        Instant createdAt,
        Instant updatedAt,
        Boolean containsMovie) {

    public static final int PREVIA = 5;

    public static MovieListDTO resumo(MovieList lista, Boolean contemFilme) {
        return de(lista, PREVIA, contemFilme);
    }

    public static MovieListDTO completa(MovieList lista) {
        return de(lista, Integer.MAX_VALUE, null);
    }

    private static MovieListDTO de(MovieList lista, int limite, Boolean contemFilme) {
        List<MovieDTO> filmes = lista.getEntries().stream()
                .limit(limite)
                .map(MovieListEntry::getMovie)
                .map(MovieDTO::new)
                .toList();
        return new MovieListDTO(lista.getId(), lista.getName(), lista.getDescription(), lista.isPublicList(),
                lista.getEntries().size(), filmes, new UserSummaryDTO(lista.getUser()),
                lista.getCreatedAt(), lista.getUpdatedAt(), contemFilme);
    }
}
