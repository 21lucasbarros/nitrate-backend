package com.lucasbarros.nitrate.dto;

import java.util.List;

/**
 * Respostas do questionário (ids do GET /taste/quiz). Em POST /taste/candidates o `movieIds` é
 * ignorado; em PUT /users/me/taste são os filmes marcados como "já vi e gostei".
 */
public record TasteRequestDTO(List<String> genres, List<String> answers, List<String> eras, List<Long> movieIds) {
}
