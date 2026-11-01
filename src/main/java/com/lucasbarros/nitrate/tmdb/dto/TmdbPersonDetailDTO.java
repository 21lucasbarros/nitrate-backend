package com.lucasbarros.nitrate.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbPersonDetailDTO {
    private Long id;
    private String name;
    private String biography;
    private String birthday;
    private String deathday;

    @JsonProperty("profile_path")
    private String profilePath;

    @JsonProperty("known_for_department")
    private String knownForDepartment;

    @JsonProperty("movie_credits")
    private Credits movieCredits = new Credits();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Credits {
        private List<TmdbPersonCreditDTO> cast = new ArrayList<>();
        private List<TmdbPersonCreditDTO> crew = new ArrayList<>();

        public List<TmdbPersonCreditDTO> getCast() { return cast; }
        public void setCast(List<TmdbPersonCreditDTO> cast) { this.cast = cast; }
        public List<TmdbPersonCreditDTO> getCrew() { return crew; }
        public void setCrew(List<TmdbPersonCreditDTO> crew) { this.crew = crew; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getBiography() { return biography; }
    public void setBiography(String biography) { this.biography = biography; }
    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }
    public String getDeathday() { return deathday; }
    public void setDeathday(String deathday) { this.deathday = deathday; }
    public String getProfilePath() { return profilePath; }
    public void setProfilePath(String profilePath) { this.profilePath = profilePath; }
    public String getKnownForDepartment() { return knownForDepartment; }
    public void setKnownForDepartment(String knownForDepartment) { this.knownForDepartment = knownForDepartment; }
    public Credits getMovieCredits() { return movieCredits; }
    public void setMovieCredits(Credits movieCredits) { this.movieCredits = movieCredits; }
}
