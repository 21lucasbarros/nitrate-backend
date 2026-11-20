package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.AdminBadgeAwardDTO;
import com.lucasbarros.nitrate.dto.BadgeDTO;
import com.lucasbarros.nitrate.dto.UserSummaryDTO;
import com.lucasbarros.nitrate.entities.BadgeType;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.entities.UserBadge;
import com.lucasbarros.nitrate.repositories.UserBadgeRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** selos do perfil: o admin concede e remove no painel; o perfil público só lê */
@Service
public class BadgeService {

    private static final int NOTA_MAX = 140;

    @Autowired private UserBadgeRepository userBadgeRepository;
    @Autowired private UserRepository userRepository;

    public List<BadgeDTO> catalogo() {
        return Arrays.stream(BadgeType.values()).map(BadgeDTO::catalogo).toList();
    }

    /** selos de um perfil, na ordem do catálogo (Fundador primeiro) */
    @Transactional(readOnly = true)
    public List<BadgeDTO> doUsuario(User user) {
        return userBadgeRepository.findByUser(user).stream()
                .filter(b -> b.getType() != null)
                .sorted(Comparator.comparing(UserBadge::getType))
                .map(BadgeDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BadgeDTO> doUsuario(String username) {
        return doUsuario(buscarUsuario(username));
    }

    @Transactional(readOnly = true)
    public List<AdminBadgeAwardDTO> todos() {
        return userBadgeRepository.findAllByOrderByAwardedAtDesc().stream()
                .filter(b -> b.getType() != null)
                .map(b -> new AdminBadgeAwardDTO(new UserSummaryDTO(b.getUser()), new BadgeDTO(b)))
                .toList();
    }

    /** concede o selo; se a pessoa já tem, só atualiza a nota */
    @Transactional
    public BadgeDTO conceder(String username, String tipo, String nota) {
        User user = buscarUsuario(username);
        BadgeType type = tipo(tipo);
        String notaLimpa = limparNota(nota);
        UserBadge badge = userBadgeRepository.findByUserAndType(user, type.name())
                .orElseGet(() -> new UserBadge(user, type, notaLimpa));
        badge.setNote(notaLimpa);
        return new BadgeDTO(userBadgeRepository.save(badge));
    }

    @Transactional
    public void remover(String username, String tipo) {
        User user = buscarUsuario(username);
        userBadgeRepository.findByUserAndType(user, tipo(tipo).name()).ifPresent(userBadgeRepository::delete);
    }

    private User buscarUsuario(String username) {
        return userRepository.findFirstByUsernameIgnoreCase(Objects.requireNonNullElse(username, "").trim())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + username));
    }

    private static BadgeType tipo(String tipo) {
        try {
            return BadgeType.valueOf(Objects.requireNonNullElse(tipo, "").trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Selo desconhecido: " + tipo);
        }
    }

    private static String limparNota(String nota) {
        if (nota == null || nota.isBlank()) return null;
        String limpa = nota.strip();
        if (limpa.length() > NOTA_MAX) {
            throw new IllegalArgumentException("A nota do selo pode ter até " + NOTA_MAX + " caracteres.");
        }
        return limpa;
    }
}
