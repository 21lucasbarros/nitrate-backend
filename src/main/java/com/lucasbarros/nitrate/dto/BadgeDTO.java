package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.BadgeType;
import com.lucasbarros.nitrate.entities.UserBadge;

import java.time.Instant;

/** selo no perfil; `note` é a nota do admin (pode ser nula), `awardedAt` nulo no catálogo */
public record BadgeDTO(String type, String label, String description, String note, Instant awardedAt) {

    public BadgeDTO(UserBadge badge) {
        this(badge.getType().name(), badge.getType().getLabel(), badge.getType().getDescription(),
                badge.getNote(), badge.getAwardedAt());
    }

    /** item do catálogo, ainda sem dono */
    public static BadgeDTO catalogo(BadgeType type) {
        return new BadgeDTO(type.name(), type.getLabel(), type.getDescription(), null, null);
    }
}
