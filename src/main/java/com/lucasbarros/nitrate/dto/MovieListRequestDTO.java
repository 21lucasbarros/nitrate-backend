package com.lucasbarros.nitrate.dto;

import java.util.List;

/** Criar ou editar uma lista: substitui tudo, e a ordem de `movieIds` é a ordem da lista. */
public record MovieListRequestDTO(String name, String description, Boolean isPublic, List<Long> movieIds) {
}
