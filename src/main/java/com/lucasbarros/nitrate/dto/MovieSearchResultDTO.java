package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.tmdb.dto.TmdbMovieSummaryDTO;

// resultado da busca no TMDB. `id` só vem preenchido se o filme já foi importado pro nosso banco
public class MovieSearchResultDTO {
    private Long id;
    private Long tmdbId;
    private String title;
    private Integer releaseYear;
    private String posterPath;
    private String overview;
    private Double rating;

    public MovieSearchResultDTO(TmdbMovieSummaryDTO resumo, Long id) {
        this.id = id;
        this.tmdbId = resumo.getId();
        this.title = resumo.getTitle();
        this.posterPath = resumo.getPosterPath();
        this.overview = resumo.getOverview();
        this.rating = resumo.getVoteAverage();
        String data = resumo.getReleaseDate();
        this.releaseYear = data != null && data.length() >= 4 ? Integer.valueOf(data.substring(0, 4)) : null;
    }

    public Long getId() { return id; }
    public Long getTmdbId() { return tmdbId; }
    public String getTitle() { return title; }
    public Integer getReleaseYear() { return releaseYear; }
    public String getPosterPath() { return posterPath; }
    public String getOverview() { return overview; }
    public Double getRating() { return rating; }
}
