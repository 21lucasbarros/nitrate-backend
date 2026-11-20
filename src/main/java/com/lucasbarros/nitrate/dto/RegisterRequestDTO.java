package com.lucasbarros.nitrate.dto;

public class RegisterRequestDTO {
    private String name;
    private String username;
    private String email;
    private String password;
    // caixa do cadastro: tem a idade mínima e aceita os Termos e a Política de Privacidade
    private Boolean acceptedTerms;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Boolean getAcceptedTerms() { return acceptedTerms; }
    public void setAcceptedTerms(Boolean acceptedTerms) { this.acceptedTerms = acceptedTerms; }
}
