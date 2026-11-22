package com.lucasbarros.nitrate.dto;

import java.util.List;

/**
 * Dados complementares de um filme, direto do TMDB: direção e elenco (com o id de cada pessoa
 * para o link do perfil), a imagem de fundo, a frase de efeito e a duração (em minutos; nula se o TMDB
 * não sabe). Nada disso fica no banco.
 */
public record MovieCreditsDTO(List<Person> directors, List<Person> cast, String backdropPath, String tagline,
                              Integer runtime) {

    /** `role`: personagem, para o elenco; nulo para a direção. `slug` nulo: o link usa o id */
    public record Person(Long tmdbId, String name, String role, String profilePath, String slug) {}
}
