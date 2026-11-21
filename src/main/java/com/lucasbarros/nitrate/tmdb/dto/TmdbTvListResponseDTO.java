package com.lucasbarros.nitrate.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbTvListResponseDTO {
    private List<TmdbTvSummaryDTO> results = new ArrayList<>();

    public List<TmdbTvSummaryDTO> getResults() { return results; }
    public void setResults(List<TmdbTvSummaryDTO> results) { this.results = results; }
}
