package com.lucasbarros.nitrate.tmdb;

import com.lucasbarros.nitrate.tmdb.dto.TmdbMovieDetailDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbPopularResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TmdbClient {

    @Autowired
    private TmdbProperties properties;

    private RestClient restClient() {
        return RestClient.builder().baseUrl(properties.getBaseURL()).build();
    }

    public TmdbPopularResponseDTO buscarFilmesPopulares(int pagina) {
        TmdbPopularResponseDTO resposta = restClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/popular")
                        .queryParam("api_key", properties.getApiKey())
                        .queryParam("language", "pt-BR")
                        .queryParam("page", pagina)
                        .build())
                .retrieve()
                .body(TmdbPopularResponseDTO.class);
        return resposta;
    }

    public TmdbMovieDetailDTO buscarDetalhesComCreditos(Long tmdbId) {
        return restClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{id}")
                        .queryParam("api_key", properties.getApiKey())
                        .queryParam("language", "pt-BR")
                        .queryParam("append_to_response", "credits")
                        .build(tmdbId))
                .retrieve()
                .body(TmdbMovieDetailDTO.class);
    }
}
