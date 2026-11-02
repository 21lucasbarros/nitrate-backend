package com.lucasbarros.nitrate.dto;

import java.time.LocalDate;
import java.util.List;

/** Importação do export do Letterboxd. O front lê o .zip e manda os filmes em lotes. */
public final class LetterboxdImportDTO {

    private LetterboxdImportDTO() {}

    public record Request(List<Item> items) {}

    /**
     * Um filme do export, já consolidado pelo front (nota, curtida, diário e review juntos).
     * Com `watchlist = true`, o filme vai para a watchlist e os outros campos são ignorados.
     */
    public record Item(
            String name,         // título como está no Letterboxd (geralmente o original/inglês)
            Integer year,
            Double rating,
            boolean liked,
            LocalDate watchedOn, // quando assistiu (diário); pode faltar
            LocalDate loggedOn,  // quando registrou no Letterboxd: vira a data da review/watchlist
            String review,
            boolean watchlist
    ) {}

    public enum Status {
        CREATED,              // review criada
        ALREADY_EXISTS,       // já havia review desse filme no Nitrate (não sobrescrevemos)
        WATCHLISTED,          // adicionado à watchlist
        ALREADY_IN_WATCHLIST,
        ALREADY_WATCHED,      // estava na watchlist do Letterboxd, mas já tem review no Nitrate
        NOT_FOUND,            // não achamos o filme no TMDB
        ERROR
    }

    public record ItemResult(String name, Integer year, Status status, Long movieId, String title, String detail) {}
}
