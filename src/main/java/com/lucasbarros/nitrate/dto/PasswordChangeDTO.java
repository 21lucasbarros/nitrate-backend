package com.lucasbarros.nitrate.dto;

/** PUT /users/me/password: a senha atual confirma que é mesmo a dona da conta. */
public record PasswordChangeDTO(String currentPassword, String newPassword) {
}
