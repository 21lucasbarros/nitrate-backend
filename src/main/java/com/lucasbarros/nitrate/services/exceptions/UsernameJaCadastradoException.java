package com.lucasbarros.nitrate.services.exceptions;

public class UsernameJaCadastradoException extends RuntimeException {
    public UsernameJaCadastradoException(String username) {
        super("Nome de usuário indisponível: " + username);
    }
}