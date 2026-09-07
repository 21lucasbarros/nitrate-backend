package com.lucasbarros.nitrate.dto;

public class LoginRequestDTO {
    private String login; // e-mail OU username
    private String password;

    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}
