package com.lucasbarros.nitrate.dto;

// aceitar o convite: com `review`, quem foi convidado escreve a própria; sem, usa a de quem convidou
public record CollaborationAcceptDTO(ReviewRequestDTO review) {
}
