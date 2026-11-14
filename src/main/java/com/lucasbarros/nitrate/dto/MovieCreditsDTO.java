package com.lucasbarros.nitrate.dto;

import java.util.List;

/** Direção e elenco de um filme, direto do TMDB (com o id de cada pessoa para o link do perfil). */
public record MovieCreditsDTO(List<Person> directors, List<Person> cast) {

    /** `role`: personagem, para o elenco; nulo para a direção */
    public record Person(Long tmdbId, String name, String role, String profilePath) {}
}
