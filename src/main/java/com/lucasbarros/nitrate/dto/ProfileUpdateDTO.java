package com.lucasbarros.nitrate.dto;

/**
 * PUT /users/me/profile. Campos vazios limpam o valor (menos o nome, que é obrigatório).
 * username nulo mantém o @ atual (clientes antigos não mandam o campo).
 */
public record ProfileUpdateDTO(String name, String username, String bio, String twitter, String instagram) {
}
