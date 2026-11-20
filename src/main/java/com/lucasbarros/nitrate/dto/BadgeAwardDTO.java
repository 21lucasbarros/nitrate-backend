package com.lucasbarros.nitrate.dto;

/** PUT /admin/users/{username}/badges/{type}: a nota pessoal que acompanha o selo (opcional) */
public record BadgeAwardDTO(String note) {
}
