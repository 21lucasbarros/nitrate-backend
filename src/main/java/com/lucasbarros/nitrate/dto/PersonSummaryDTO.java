package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.tmdb.dto.TmdbPersonSearchResponseDTO;

import java.util.ArrayList;
import java.util.List;

public class PersonSummaryDTO {
    private Long tmdbId;
    private String name;
    private String profilePath;
    private String slug; // nulo: o link usa o id
    private String department; // "Acting", "Directing"... (o front traduz)
    private List<String> knownFor = new ArrayList<>();

    public PersonSummaryDTO(TmdbPersonSearchResponseDTO.Result pessoa, String slug) {
        this.tmdbId = pessoa.getId();
        this.slug = slug;
        this.name = pessoa.getName();
        this.profilePath = pessoa.getProfilePath();
        this.department = pessoa.getKnownForDepartment();
        for (TmdbPersonSearchResponseDTO.KnownFor obra : pessoa.getKnownFor()) {
            if (!"movie".equals(obra.getMediaType())) continue; // ignora séries
            knownFor.add(obra.getTitle());
            if (knownFor.size() == 3) break;
        }
    }

    public Long getTmdbId() { return tmdbId; }
    public String getName() { return name; }
    public String getProfilePath() { return profilePath; }
    public String getSlug() { return slug; }
    public String getDepartment() { return department; }
    public List<String> getKnownFor() { return knownFor; }
}
