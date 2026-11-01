package com.lucasbarros.nitrate.dto;

public class AuthResponseDTO {
    private String token;
    private String name;
    private String username;
    private String email;

    public AuthResponseDTO(String token, String name, String username, String email) {
        this.token = token;
        this.name = name;
        this.username = username;
        this.email = email;
    }

    public String getToken() { return token; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
}
