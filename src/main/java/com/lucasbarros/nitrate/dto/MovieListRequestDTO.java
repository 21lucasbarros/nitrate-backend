package com.lucasbarros.nitrate.dto;

import java.util.List;

/**
 * Criar ou editar uma lista: substitui tudo. `items` (filmes e séries, na ordem da lista) tem
 * prioridade; sem ele, vale `movieIds` (clientes antigos, como o app), e as séries da lista ficam.
 */
public record MovieListRequestDTO(String name, String description, Boolean isPublic, List<Long> movieIds,
                                  List<Item> items) {

    /** kind: "MOVIE" ou "SERIES" */
    public record Item(String kind, Long id) {}

    public MovieListRequestDTO(String name, String description, Boolean isPublic, List<Long> movieIds) {
        this(name, description, isPublic, movieIds, null);
    }
}
