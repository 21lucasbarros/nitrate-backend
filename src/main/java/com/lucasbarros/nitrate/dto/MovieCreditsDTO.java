package com.lucasbarros.nitrate.dto;

import java.util.List;

/**
 * Dados complementares de um filme, direto do TMDB: direção e elenco (com o id de cada pessoa
 * para o link do perfil), a imagem de fundo e a frase de efeito. Nada disso fica no banco.
 */
public record MovieCreditsDTO(List<Person> directors, List<Person> cast, String backdropPath, String tagline) {

    /** `role`: personagem, para o elenco; nulo para a direção */
    public record Person(Long tmdbId, String name, String role, String profilePath) {}
}
