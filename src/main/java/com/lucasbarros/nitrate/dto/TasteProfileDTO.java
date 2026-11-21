package com.lucasbarros.nitrate.dto;

import java.time.Instant;
import java.util.List;

/** GET/PUT /users/me/taste: o que a pessoa respondeu; `updatedAt` nulo = nunca respondeu */
public record TasteProfileDTO(List<String> genres, List<String> answers, List<String> eras, List<MovieDTO> movies,
                              Instant updatedAt) {

    public static TasteProfileDTO vazio() {
        return new TasteProfileDTO(List.of(), List.of(), List.of(), List.of(), null);
    }
}
