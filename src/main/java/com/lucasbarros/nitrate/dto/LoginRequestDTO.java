package com.lucasbarros.nitrate.dto;

public class LoginRequestDTO {
    private String login; // e-mail OU username
    private String password;
    private Boolean rememberMe; // "Lembrar de mim": token de 30 dias em vez de 24h

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Boolean getRememberMe() { return rememberMe; }
    public void setRememberMe(Boolean rememberMe) { this.rememberMe = rememberMe; }
}
