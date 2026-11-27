package com.lucasbarros.nitrate.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** A conta logada na requisição atual, para quem monta respostas sem receber o usuário (DTOs). */
public final class UsuarioAtual {

    private UsuarioAtual() {
    }

    /** o id da conta logada; nulo sem login (ou fora de uma requisição) */
    public static Long id() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao != null && autenticacao.getPrincipal() instanceof CustomUserDetails detalhes
                ? detalhes.getUser().getId()
                : null;
    }
}
