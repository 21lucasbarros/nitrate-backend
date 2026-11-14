package com.lucasbarros.nitrate.dto;

import java.util.List;

/** Onde assistir um filme no Brasil. `link` é a página do TMDB com os links de cada serviço. */
public record WatchProvidersDTO(
        String link,
        List<Provider> streaming,
        List<Provider> free,  // grátis, inclusive com anúncios
        List<Provider> rent,
        List<Provider> buy
) {
    public record Provider(Long id, String name, String logoPath) {}

    public static WatchProvidersDTO vazio() {
        return new WatchProvidersDTO(null, List.of(), List.of(), List.of(), List.of());
    }
}
