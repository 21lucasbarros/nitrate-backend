package com.lucasbarros.nitrate.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/** /movie/{id}/release_dates: datas de estreia por país e por tipo de lançamento. */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbReleaseDatesDTO {
    // tipos do TMDB: 1 première, 2 cinema (limitado), 3 cinema, 4 digital, 5 físico, 6 TV
    public static final int CINEMA_LIMITADO = 2;
    public static final int CINEMA = 3;

    private List<Country> results = new ArrayList<>();

    public List<Country> getResults() { return results; }
    public void setResults(List<Country> results) { this.results = results; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Country {
        @JsonProperty("iso_3166_1")
        private String pais;

        @JsonProperty("release_dates")
        private List<Release> lancamentos = new ArrayList<>();

        public String getPais() { return pais; }
        public void setPais(String pais) { this.pais = pais; }
        public List<Release> getLancamentos() { return lancamentos; }
        public void setLancamentos(List<Release> lancamentos) { this.lancamentos = lancamentos; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Release {
        private int type;

        @JsonProperty("release_date")
        private String data; // ISO com horário: "2026-05-01T00:00:00.000Z"

        public int getType() { return type; }
        public void setType(int type) { this.type = type; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
    }
}
