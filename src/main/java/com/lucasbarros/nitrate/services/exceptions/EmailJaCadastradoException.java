package com.lucasbarros.nitrate.services.exceptions;

public class EmailJaCadastradoException extends RuntimeException {
    public EmailJaCadastradoException(String email) {
        super("Já existe uma conta com o e-mail: " + email);
    }
}
