package com.lucasbarros.nitrate.dto;

import java.util.Map;

/**
 * Como o filme foi recebido no Nitrate. Cada pessoa conta uma vez: se assistiu mais de uma
 * vez, vale a nota da vez mais recente.
 */
public record MovieStatsDTO(
        Double averageRating,                    // null se ninguém deu nota
        int ratingsCount,
        int watchedCount,                        // pessoas que registraram o filme
        int likesCount,
        Map<String, Integer> ratingDistribution  // "0.5" .. "5.0" -> pessoas
) {}
