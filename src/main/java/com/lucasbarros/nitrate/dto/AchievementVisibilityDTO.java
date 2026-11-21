package com.lucasbarros.nitrate.dto;

/** PUT /users/me/achievements/{id}: mostrar ou tirar a conquista do perfil */
public record AchievementVisibilityDTO(boolean hidden) {
}
