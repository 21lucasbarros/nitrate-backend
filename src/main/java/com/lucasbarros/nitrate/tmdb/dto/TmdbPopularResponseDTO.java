package com.lucasbarros.nitrate.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbPopularResponseDTO {
    private List<TmdbMovieSummaryDTO> results = new ArrayList<>();

    public List<TmdbMovieSummaryDTO> getResults() {
        return results;
    }
    public void setResults(List<TmdbMovieSummaryDTO> results) { this.results = results; }
}
