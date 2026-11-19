package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.User;

// dados públicos mínimos de um usuário, usados dentro de outras respostas (ex.: autor da review)
public class UserSummaryDTO {
    private String username;
    private String name;
    private String avatar; // caminho da foto na API (ex.: /users/fulano/avatar?v=...); nulo sem foto

    public UserSummaryDTO(User user) {
        this.username = user.getUsername();
        this.name = user.getName();
        this.avatar = user.getAvatarPath();
    }

    public String getUsername() { return username; }
    public String getName() { return name; }
    public String getAvatar() { return avatar; }
}
