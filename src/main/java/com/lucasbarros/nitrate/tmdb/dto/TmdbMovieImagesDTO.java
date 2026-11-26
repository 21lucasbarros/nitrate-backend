package com.lucasbarros.nitrate.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/** imagens de um filme no TMDB (append_to_response=images): as cenas (backdrops) e os pôsteres */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbMovieImagesDTO {
    private List<Image> backdrops = new ArrayList<>();
    private List<Image> posters = new ArrayList<>();

    public List<Image> getBackdrops() { return backdrops; }
    public void setBackdrops(List<Image> backdrops) { this.backdrops = backdrops; }
    public List<Image> getPosters() { return posters; }
    public void setPosters(List<Image> posters) { this.posters = posters; }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Image {
        @JsonProperty("file_path")
        private String filePath;
        private Integer width;
        private Integer height;
        // idioma do texto na imagem; nulo = cena sem texto, a melhor para fundo
        @JsonProperty("iso_639_1")
        private String language;
        @JsonProperty("vote_average")
        private Double voteAverage;

        public String getFilePath() { return filePath; }
        public void setFilePath(String filePath) { this.filePath = filePath; }
        public Integer getWidth() { return width; }
        public void setWidth(Integer width) { this.width = width; }
        public Integer getHeight() { return height; }
        public void setHeight(Integer height) { this.height = height; }
        public String getLanguage() { return language; }
        public void setLanguage(String language) { this.language = language; }
        public Double getVoteAverage() { return voteAverage; }
        public void setVoteAverage(Double voteAverage) { this.voteAverage = voteAverage; }
    }
}
