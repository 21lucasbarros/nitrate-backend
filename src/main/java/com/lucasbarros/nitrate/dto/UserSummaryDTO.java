package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.User;

// dados públicos mínimos de um usuário, usados dentro de outras respostas (ex.: autor da review)
public class UserSummaryDTO {
    private String username;
    private String name;

    public UserSummaryDTO(User user) {
        this.username = user.getUsername();
        this.name = user.getName();
    }

    public String getUsername() { return username; }
    public String getName() { return name; }
}
