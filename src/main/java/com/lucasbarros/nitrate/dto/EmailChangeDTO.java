package com.lucasbarros.nitrate.dto;

/** PUT /users/me/email: a senha atual confirma que é mesmo a dona da conta. */
public record EmailChangeDTO(String email, String password) {
}
