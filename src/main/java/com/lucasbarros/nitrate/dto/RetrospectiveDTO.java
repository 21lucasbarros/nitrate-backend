package com.lucasbarros.nitrate.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** Retrospectiva de um mês do diário: tudo calculado a partir das reviews, nada é salvo. */
public record RetrospectiveDTO(
        String username,
        String name,
        String month,                           // "2026-10"
        int filmsCount,
        int reviewsCount,                       // reviews com texto
        int likesCount,
        Double averageRating,                   // null se nenhum filme teve nota
        Map<String, Integer> ratingDistribution, // "0.5" .. "5.0" -> quantidade
        List<RankedItem> topDirectors,
        List<RankedItem> topGenres,
        List<RankedItem> topActors,             // só atores que aparecem em 2+ filmes do mês
        List<Film> favorites,                   // melhores notas (curtida desempata)
        List<Film> films                        // o mês inteiro, em ordem de quando assistiu
) {
    public record RankedItem(String name, int count) {}

    public record Film(MovieDTO movie, Double rating, boolean liked, LocalDate watchedOn) {}
}
