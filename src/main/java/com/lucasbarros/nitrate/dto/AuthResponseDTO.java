package com.lucasbarros.nitrate.dto;

public class AuthResponseDTO {
    private String token;
    private String name;
    private String email;

    public AuthResponseDTO(String token, String name, String email) {
        this.token = token;
        this.name = name;
        this.email = email;
    }

    public String getToken() { return token; }
    public String getName() { return name; }
    public String getEmail() { return email; }
}
