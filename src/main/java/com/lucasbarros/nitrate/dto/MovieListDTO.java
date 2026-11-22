package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.MovieList;
import com.lucasbarros.nitrate.entities.MovieListEntry;
import com.lucasbarros.nitrate.services.NitrateAccountService;

import java.time.Instant;
import java.util.List;

/**
 * Uma lista de filmes e séries. Nas listagens (perfil, "adicionar à lista") `items` traz só os
 * primeiros, para a prévia; na página da lista, todos em ordem. `movies` é o mesmo recorte só com os
 * filmes (para clientes antigos, como o app); `filmsCount` conta filmes e séries, `seriesCount` só as séries.
 * `containsMovie` só vem preenchido em /users/me/lists?movieId=. Curtidas, comentários e se quem
 * vê curtiu ou salvou vêm onde a lista aparece para outras pessoas (perfil, página, home).
 * `official`: lista do Nitrate (da conta de sistema, criada pelo admin), com selo no front.
 */
public record MovieListDTO(
        Long id,
        String slug,
        String name,
        String description,
        boolean isPublic,
        int filmsCount,
        int seriesCount,
        List<MovieDTO> movies,
        List<ListItemDTO> items,
        UserSummaryDTO owner,
        boolean official,
        Instant createdAt,
        Instant updatedAt,
        Boolean containsMovie,
        long likesCount,
        long commentsCount,
        Boolean likedByMe,
        Boolean savedByMe) {

    public static final int PREVIA = 5;

    public static MovieListDTO resumo(MovieList lista, Boolean contemFilme) {
        return de(lista, PREVIA, contemFilme, ListEngagementDTO.VAZIO);
    }

    public static MovieListDTO resumo(MovieList lista, ListEngagementDTO engajamento) {
        return de(lista, PREVIA, null, engajamento);
    }

    public static MovieListDTO completa(MovieList lista, ListEngagementDTO engajamento) {
        return de(lista, Integer.MAX_VALUE, null, engajamento);
    }

    private static MovieListDTO de(MovieList lista, int limite, Boolean contemFilme, ListEngagementDTO e) {
        List<MovieListEntry> recorte = lista.getEntries().stream().limit(limite).toList();
        List<ListItemDTO> itens = recorte.stream().map(ListItemDTO::de).toList();
        List<MovieDTO> filmes = recorte.stream()
                .filter(entrada -> !entrada.isSeries())
                .map(MovieListEntry::getMovie)
                .map(MovieDTO::new)
                .toList();
        return new MovieListDTO(lista.getId(), lista.getSlug(), lista.getName(), lista.getDescription(),
                lista.isPublicList(), lista.getEntries().size(),
                (int) lista.getEntries().stream().filter(MovieListEntry::isSeries).count(), filmes, itens, new UserSummaryDTO(lista.getUser()),
                NitrateAccountService.ehConta(lista.getUser()),
                lista.getCreatedAt(), lista.getUpdatedAt(), contemFilme,
                e.likesCount(), e.commentsCount(), e.likedByMe(), e.savedByMe());
    }
}
