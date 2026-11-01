package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.tmdb.dto.TmdbPersonDetailDTO;

import java.util.List;

public class PersonDetailDTO {
    private Long tmdbId;
    private String name;
    private String profilePath;
    private String department;
    private String biography;
    private String birthday;
    private String deathday;
    private List<PersonCreditDTO> directed;
    private List<PersonCreditDTO> acted;

    public PersonDetailDTO(TmdbPersonDetailDTO pessoa, List<PersonCreditDTO> directed, List<PersonCreditDTO> acted) {
        this.tmdbId = pessoa.getId();
        this.name = pessoa.getName();
        this.profilePath = pessoa.getProfilePath();
        this.department = pessoa.getKnownForDepartment();
        this.biography = pessoa.getBiography();
        this.birthday = pessoa.getBirthday();
        this.deathday = pessoa.getDeathday();
        this.directed = directed;
        this.acted = acted;
    }

    public Long getTmdbId() { return tmdbId; }
    public String getName() { return name; }
    public String getProfilePath() { return profilePath; }
    public String getDepartment() { return department; }
    public String getBiography() { return biography; }
    public String getBirthday() { return birthday; }
    public String getDeathday() { return deathday; }
    public List<PersonCreditDTO> getDirected() { return directed; }
    public List<PersonCreditDTO> getActed() { return acted; }
}
