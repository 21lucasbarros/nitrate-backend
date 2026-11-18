package com.lucasbarros.nitrate.dto;

import java.util.List;

/** a lista completa (atores ou diretores) com ids do TMDB, já na ordem (substitui a anterior) */
public record FavoritePeopleRequestDTO(List<Long> tmdbIds) {}
