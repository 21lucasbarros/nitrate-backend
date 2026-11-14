package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.tmdb.dto.TmdbPersonCreditDTO;

// filme na filmografia de uma pessoa. `id` só existe se o filme já está no nosso banco
public class PersonCreditDTO {
    private Long id;
    private String slug;
    private Long tmdbId;
    private String title;
    private Integer releaseYear;
    private String posterPath;
    private String role; // personagem (elenco) ou função (equipe)

    public PersonCreditDTO(TmdbPersonCreditDTO credito, Movie local) {
        this.id = local == null ? null : local.getId();
        this.slug = local == null ? null : local.getSlug();
        this.tmdbId = credito.getId();
        this.title = credito.getTitle();
        this.posterPath = credito.getPosterPath();
        this.role = credito.getCharacter() != null ? credito.getCharacter() : credito.getJob();
        String data = credito.getReleaseDate();
        this.releaseYear = data != null && data.length() >= 4 ? Integer.valueOf(data.substring(0, 4)) : null;
    }

    public Long getId() { return id; }
    public String getSlug() { return slug; }
    public Long getTmdbId() { return tmdbId; }
    public String getTitle() { return title; }
    public Integer getReleaseYear() { return releaseYear; }
    public String getPosterPath() { return posterPath; }
    public String getRole() { return role; }
}
