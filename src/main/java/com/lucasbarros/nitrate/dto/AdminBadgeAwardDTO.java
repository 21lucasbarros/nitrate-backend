package com.lucasbarros.nitrate.dto;

/** um selo concedido, com quem recebeu (lista de selos do painel) */
public record AdminBadgeAwardDTO(UserSummaryDTO user, BadgeDTO badge) {
}
