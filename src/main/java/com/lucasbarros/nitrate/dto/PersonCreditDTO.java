package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.tmdb.dto.TmdbPersonCreditDTO;

// filme na filmografia de uma pessoa. `id` só existe se o filme já está no nosso banco
public class PersonCreditDTO {
    // abaixo disso a média do TMDB é ruído (um ou dois votos)
    private static final int VOTOS_MINIMOS_TMDB = 5;

    private Long id;
    private String slug;
    private Long tmdbId;
    private String title;
    private Integer releaseYear;
    private String releaseDate; // yyyy-mm-dd, para ordenar por lançamento
    private String posterPath;
    private String role; // personagem (elenco) ou função (equipe)
    private Double tmdbRating; // 0 a 10; null com poucos votos
    private int tmdbVotes;
    private Double nitrateRating; // média de 0,5 a 5 das notas no Nitrate; null se ninguém deu nota
    private long nitrateRatings;

    public PersonCreditDTO(TmdbPersonCreditDTO credito, Movie local, Double nitrateRating, long nitrateRatings) {
        this.id = local == null ? null : local.getId();
        this.slug = local == null ? null : local.getSlug();
        this.tmdbId = credito.getId();
        this.title = credito.getTitle();
        this.posterPath = credito.getPosterPath();
        this.role = credito.getCharacter() != null ? credito.getCharacter() : credito.getJob();
        String data = credito.getReleaseDate();
        this.releaseDate = data == null || data.isEmpty() ? null : data;
        this.releaseYear = data != null && data.length() >= 4 ? Integer.valueOf(data.substring(0, 4)) : null;
        this.tmdbVotes = credito.getVoteCount();
        this.tmdbRating = tmdbVotes >= VOTOS_MINIMOS_TMDB ? credito.getVoteAverage() : null;
        this.nitrateRating = nitrateRating;
        this.nitrateRatings = nitrateRatings;
    }

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public Long getTmdbId() { return tmdbId; }
    public String getTitle() { return title; }
    public Integer getReleaseYear() { return releaseYear; }
    public String getReleaseDate() { return releaseDate; }
    public String getPosterPath() { return posterPath; }
    public String getRole() { return role; }
    public Double getTmdbRating() { return tmdbRating; }
    public int getTmdbVotes() { return tmdbVotes; }
    public Double getNitrateRating() { return nitrateRating; }
    public long getNitrateRatings() { return nitrateRatings; }
}
