package com.lucasbarros.nitrate.dto;

/** DELETE /users/me: a senha confirma que é mesmo a dona da conta (e não um token esquecido aberto). */
public record AccountDeletionDTO(String password) {
}
