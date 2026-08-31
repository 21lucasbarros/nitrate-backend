package com.lucasbarros.nitrate.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbCreditsDTO {
    private List<TmdbCrewMemberDTO> cast = new ArrayList<>();
    private List<TmdbCrewMemberDTO> crew = new ArrayList<>();

    public List<TmdbCrewMemberDTO> getCast() {
        return cast;
    }

    public void setCast(List<TmdbCrewMemberDTO> cast) {
        this.cast = cast;
    }

    public List<TmdbCrewMemberDTO> getCrew() {
        return crew;
    }

    public void setCrew(List<TmdbCrewMemberDTO> crew) {
        this.crew = crew;
    }
}
