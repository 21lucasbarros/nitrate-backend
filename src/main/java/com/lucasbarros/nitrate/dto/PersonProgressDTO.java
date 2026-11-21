package com.lucasbarros.nitrate.dto;

/**
 * Quanto da filmografia de uma pessoa você já viu: `watched` de `total` filmes já lançados.
 * `achievement` existe quando a filmografia completa virou conquista (pode ser de antes de a
 * pessoa lançar filmes novos, então nem sempre `watched == total`).
 */
public record PersonProgressDTO(int watched, int total, AchievementDTO achievement) {
}
