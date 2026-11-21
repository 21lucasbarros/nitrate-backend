package com.lucasbarros.nitrate.dto;

import java.util.List;

/** POST /taste/candidates: os filmes da grade, separados pela preferência que os trouxe */
public record TasteCandidateGroupDTO(String id, String label, List<MovieDTO> movies) {
}
