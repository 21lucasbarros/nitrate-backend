package com.lucasbarros.nitrate.dto;

// aceitar o convite: com `review`, quem foi convidado escreve o próprio registro; sem, usa o de quem convidou
public record SeriesCollaborationAcceptDTO(SeriesReviewRequestDTO review) {
}
