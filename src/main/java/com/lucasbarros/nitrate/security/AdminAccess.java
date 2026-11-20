package com.lucasbarros.nitrate.security;

import com.lucasbarros.nitrate.entities.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Quem pode abrir o painel de admin: as contas de ADMIN_EMAILS ou de ADMIN_USERNAMES (separados
 * por vírgula). O e-mail é mais seguro: o @ pode ser trocado nas configurações e, se o admin trocar
 * o dele, outra pessoa poderia se cadastrar com o @ antigo.
 */
@Component
public class AdminAccess {

    private final Set<String> emails;
    private final Set<String> usernames;

    public AdminAccess(@Value("${admin.emails:}") List<String> emails,
                       @Value("${admin.usernames:}") List<String> usernames) {
        this.emails = normalizar(emails);
        // aceita "@fulano" ou "fulano"; sem diferenciar maiúsculas, como o resto do site
        this.usernames = normalizar(usernames.stream().map(u -> u.trim().replaceFirst("^@", "")).toList());
    }

    public boolean isAdmin(User user) {
        if (user == null) return false;
        return (user.getEmail() != null && emails.contains(user.getEmail().toLowerCase(Locale.ROOT)))
                || (user.getUsername() != null && usernames.contains(user.getUsername().toLowerCase(Locale.ROOT)));
    }

    private static Set<String> normalizar(List<String> valores) {
        return valores.stream()
                .map(v -> v.trim().toLowerCase(Locale.ROOT))
                .filter(v -> !v.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }
}
