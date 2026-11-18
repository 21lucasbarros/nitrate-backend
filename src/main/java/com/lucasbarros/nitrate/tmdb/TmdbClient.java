package com.lucasbarros.nitrate.tmdb;

import com.lucasbarros.nitrate.tmdb.dto.TmdbMovieDetailDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbPersonDetailDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbPersonSearchResponseDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbPopularResponseDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbReleaseDatesDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbWatchProvidersDTO;
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

    public TmdbPopularResponseDTO pesquisarFilmes(String termo) {
        return pesquisarFilmes(termo, null);
    }

    /**
     * com `ano`, só traz filmes com alguma estreia naquele ano (essencial para títulos repetidos).
     * `year` considera qualquer data de lançamento (festival, outro país); `primary_release_year`
     * só a principal, e deixaria de fora filmes como Obsession (2025 no festival, 2026 no circuito).
     */
    public TmdbPopularResponseDTO pesquisarFilmes(String termo, Integer ano) {
        return restClient().get()
                .uri(uriBuilder -> {
                    uriBuilder
                            .path("/search/movie")
                            .queryParam("api_key", properties.getApiKey())
                            .queryParam("language", "pt-BR")
                            .queryParam("include_adult", false)
                            .queryParam("query", termo);
                    if (ano != null) uriBuilder.queryParam("year", ano);
                    return uriBuilder.build();
                })
                .retrieve()
                .body(TmdbPopularResponseDTO.class);
    }

    public TmdbPersonSearchResponseDTO pesquisarPessoas(String termo) {
        return restClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/person")
                        .queryParam("api_key", properties.getApiKey())
                        .queryParam("language", "pt-BR")
                        .queryParam("include_adult", false)
                        .queryParam("query", termo)
                        .build())
                .retrieve()
                .body(TmdbPersonSearchResponseDTO.class);
    }

    /** só nome e foto, sem a filmografia (para guardar favoritos) */
    public TmdbPersonDetailDTO buscarPessoa(Long personId) {
        return restClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/person/{id}")
                        .queryParam("api_key", properties.getApiKey())
                        .queryParam("language", "pt-BR")
                        .build(personId))
                .retrieve()
                .body(TmdbPersonDetailDTO.class);
    }

    public TmdbPersonDetailDTO buscarPessoaComFilmografia(Long personId) {
        return restClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/person/{id}")
                        .queryParam("api_key", properties.getApiKey())
                        .queryParam("language", "pt-BR")
                        .queryParam("append_to_response", "movie_credits")
                        .build(personId))
                .retrieve()
                .body(TmdbPersonDetailDTO.class);
    }

    public TmdbReleaseDatesDTO buscarDatasDeLancamento(Long tmdbId) {
        return restClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{id}/release_dates")
                        .queryParam("api_key", properties.getApiKey())
                        .build(tmdbId))
                .retrieve()
                .body(TmdbReleaseDatesDTO.class);
    }

    public TmdbWatchProvidersDTO buscarOndeAssistir(Long tmdbId) {
        return restClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{id}/watch/providers")
                        .queryParam("api_key", properties.getApiKey())
                        .build(tmdbId))
                .retrieve()
                .body(TmdbWatchProvidersDTO.class);
    }
}
