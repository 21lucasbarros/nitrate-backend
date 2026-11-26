package com.lucasbarros.nitrate.dto;

import java.util.List;

/**
 * GET /movies/{id}/posters e /series/{id}/posters: os pôsteres de um filme ou de uma série para
 * escolher o das reviews de quem registrou (o do título primeiro)
 */
public record PostersDTO(Long id, String defaultPoster, List<String> posters) {
}
