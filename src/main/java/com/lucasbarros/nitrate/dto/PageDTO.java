package com.lucasbarros.nitrate.dto;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/** Página de resultados. `page` começa em 0. */
public record PageDTO<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <E, T> PageDTO<T> of(Page<E> pagina, Function<E, T> conversor) {
        return new PageDTO<>(
                pagina.getContent().stream().map(conversor).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages());
    }
}
