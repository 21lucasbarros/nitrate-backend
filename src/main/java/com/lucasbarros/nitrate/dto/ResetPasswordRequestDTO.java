package com.lucasbarros.nitrate.dto;

public class ResetPasswordRequestDTO {
    private String token; // o do link enviado por e-mail
    private String password;

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
