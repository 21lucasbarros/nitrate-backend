package com.lucasbarros.nitrate.dto;

import java.util.List;

/** a lista completa de favoritos, já na ordem (substitui a anterior) */
public record FavoritesRequestDTO(List<Long> movieIds) {}
