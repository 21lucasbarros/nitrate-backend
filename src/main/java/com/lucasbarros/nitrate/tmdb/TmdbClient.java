package com.lucasbarros.nitrate.tmdb;

import com.lucasbarros.nitrate.tmdb.dto.TmdbMovieDetailDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbMovieImagesDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbPersonDetailDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbPersonSearchResponseDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbPopularResponseDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbReleaseDatesDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbTvDetailDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbTvListResponseDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbTvSeasonDTO;
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

    /**
     * Filmes com uma palavra-chave do TMDB, dos mais populares para os menos (ex.: 4344 = "musical",
     * que o TMDB usa no lugar de um gênero). Só com algum número de votos, para ficar nos conhecidos.
     */
    public TmdbPopularResponseDTO descobrirPorPalavraChave(int palavraChave, int pagina) {
        return restClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/discover/movie")
                        .queryParam("api_key", properties.getApiKey())
                        .queryParam("with_keywords", palavraChave)
                        .queryParam("sort_by", "popularity.desc")
                        .queryParam("include_adult", false)
                        .queryParam("vote_count.gte", 20)
                        .queryParam("page", pagina)
                        .build())
                .retrieve()
                .body(TmdbPopularResponseDTO.class);
    }

    /** só os detalhes do filme (sem elenco): para preencher dados que faltam, como a data de estreia */
    public TmdbMovieDetailDTO buscarDetalhes(Long tmdbId) {
        return restClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{id}")
                        .queryParam("api_key", properties.getApiKey())
                        .queryParam("language", "pt-BR")
                        .build(tmdbId))
                .retrieve()
                .body(TmdbMovieDetailDTO.class);
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

    /**
     * detalhes com as cenas do filme (backdrops) para escolher o fundo do perfil. O idioma das
     * imagens fica sem filtro de pt-BR: as cenas sem texto (null) são a maioria e as melhores.
     */
    public TmdbMovieDetailDTO buscarDetalhesComImagens(Long tmdbId) {
        return restClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{id}")
                        .queryParam("api_key", properties.getApiKey())
                        .queryParam("language", "pt-BR")
                        .queryParam("append_to_response", "images")
                        .queryParam("include_image_language", "null,pt,en")
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

    /** nome em inglês, para o slug de quem o TMDB devolve no alfabeto original (ex.: diretores gregos) */
    public TmdbPersonDetailDTO buscarPessoaEmIngles(Long personId) {
        return restClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/person/{id}")
                        .queryParam("api_key", properties.getApiKey())
                        .queryParam("language", "en-US")
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
                        .queryParam("append_to_response", "movie_credits,tv_credits")
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

    // ---- séries (mesma chave e mesmos limites do TMDB; nada a pagar) ----

    public TmdbTvListResponseDTO pesquisarSeries(String termo) {
        return restClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/tv")
                        .queryParam("api_key", properties.getApiKey())
                        .queryParam("language", "pt-BR")
                        .queryParam("include_adult", false)
                        .queryParam("query", termo)
                        .build())
                .retrieve()
                .body(TmdbTvListResponseDTO.class);
    }

    /** as séries mais vistas da semana no TMDB */
    public TmdbTvListResponseDTO seriesEmAlta() {
        return restClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/trending/tv/week")
                        .queryParam("api_key", properties.getApiKey())
                        .queryParam("language", "pt-BR")
                        .build())
                .retrieve()
                .body(TmdbTvListResponseDTO.class);
    }

    public TmdbTvListResponseDTO seriesMaisBemAvaliadas() {
        return restClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tv/top_rated")
                        .queryParam("api_key", properties.getApiKey())
                        .queryParam("language", "pt-BR")
                        .build())
                .retrieve()
                .body(TmdbTvListResponseDTO.class);
    }

    /** detalhes da série com o elenco de todas as temporadas */
    public TmdbTvDetailDTO buscarSerie(Long tmdbId) {
        return restClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tv/{id}")
                        .queryParam("api_key", properties.getApiKey())
                        .queryParam("language", "pt-BR")
                        .queryParam("append_to_response", "aggregate_credits")
                        .build(tmdbId))
                .retrieve()
                .body(TmdbTvDetailDTO.class);
    }

    /** só os dados da série, sem o elenco (Radar de Estreias) */
    public TmdbTvDetailDTO buscarSerieSemElenco(Long tmdbId) {
        return restClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tv/{id}")
                        .queryParam("api_key", properties.getApiKey())
                        .queryParam("language", "pt-BR")
                        .build(tmdbId))
                .retrieve()
                .body(TmdbTvDetailDTO.class);
    }

    /** as cenas da série (backdrops), sem filtro de pt-BR: as sem texto (null) são a maioria e as melhores */
    public TmdbMovieImagesDTO buscarImagensSerie(Long tmdbId) {
        return restClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tv/{id}/images")
                        .queryParam("api_key", properties.getApiKey())
                        .queryParam("include_image_language", "null,pt,en")
                        .build(tmdbId))
                .retrieve()
                .body(TmdbMovieImagesDTO.class);
    }

    public TmdbTvSeasonDTO buscarTemporada(Long tmdbId, int temporada) {
        return restClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tv/{id}/season/{season}")
                        .queryParam("api_key", properties.getApiKey())
                        .queryParam("language", "pt-BR")
                        .build(tmdbId, temporada))
                .retrieve()
                .body(TmdbTvSeasonDTO.class);
    }

    public TmdbWatchProvidersDTO buscarOndeAssistirSerie(Long tmdbId) {
        return restClient().get()
                .uri(uriBuilder -> uriBuilder
                        .path("/tv/{id}/watch/providers")
                        .queryParam("api_key", properties.getApiKey())
                        .build(tmdbId))
                .retrieve()
                .body(TmdbWatchProvidersDTO.class);
    }
}
