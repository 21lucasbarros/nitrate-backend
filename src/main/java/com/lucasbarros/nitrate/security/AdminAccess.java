package com.lucasbarros.nitrate.security;

import com.lucasbarros.nitrate.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Quem pode abrir o painel de admin: as contas de ADMIN_USER_IDS (ids separados por vírgula). Pelo
 * id, que nunca muda nem é reaproveitado: pelo @ ou pelo e-mail, quem se cadastrasse com o @ ou o
 * e-mail de um admin que trocou o dele (ou com um que ainda não tem conta) viraria admin.
 */
@Component
public class AdminAccess {

    private final Set<Long> ids;

    public AdminAccess(@Value("${admin.user-ids:}") List<String> ids) {
        this.ids = ids.stream()
                .map(String::trim)
                .filter(id -> !id.isEmpty())
                .map(Long::valueOf)
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAdmin(User user) {
        return user != null && user.getId() != null && ids.contains(user.getId());
    }
}
