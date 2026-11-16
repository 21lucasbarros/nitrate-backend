package com.lucasbarros.nitrate.dto;

/** PUT /users/me/profile. Campos vazios limpam o valor (menos o nome, que é obrigatório). */
public record ProfileUpdateDTO(String name, String bio, String twitter, String instagram) {
}
