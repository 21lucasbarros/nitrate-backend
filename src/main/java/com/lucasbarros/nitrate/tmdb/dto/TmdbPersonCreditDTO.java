package com.lucasbarros.nitrate.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

// um filme ou uma série na filmografia de uma pessoa (como elenco ou equipe)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbPersonCreditDTO {
    private Long id;
    private String title;
    private String name;      // só nas séries, no lugar do title
    private String character; // só no elenco
    private String job;       // só na equipe (Director, Writer...)
    private Integer order;    // só no elenco: posição nos créditos

    @JsonProperty("release_date")
    private String releaseDate;

    @JsonProperty("first_air_date")
    private String firstAirDate; // só nas séries, no lugar do release_date

    @JsonProperty("episode_count")
    private Integer episodeCount; // só nas séries

    @JsonProperty("genre_ids")
    private List<Integer> genreIds = new ArrayList<>();

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("vote_count")
    private Integer voteCount;

    @JsonProperty("vote_average")
    private Double voteAverage;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    /** o título do filme ou o nome da série */
    public String getTitle() { return title != null ? title : name; }
    public void setTitle(String title) { this.title = title; }
    public void setName(String name) { this.name = name; }
    public String getCharacter() { return character; }
    public void setCharacter(String character) { this.character = character; }
    public String getJob() { return job; }
    public void setJob(String job) { this.job = job; }
    public Integer getOrder() { return order; }
    public void setOrder(Integer order) { this.order = order; }
    /** a estreia do filme ou da série */
    public String getReleaseDate() { return releaseDate != null ? releaseDate : firstAirDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    public void setFirstAirDate(String firstAirDate) { this.firstAirDate = firstAirDate; }
    public Integer getEpisodeCount() { return episodeCount; }
    public void setEpisodeCount(Integer episodeCount) { this.episodeCount = episodeCount; }
    public List<Integer> getGenreIds() { return genreIds; }
    public void setGenreIds(List<Integer> genreIds) { this.genreIds = genreIds == null ? new ArrayList<>() : genreIds; }
    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
    public int getVoteCount() { return voteCount == null ? 0 : voteCount; }
    public void setVoteCount(Integer voteCount) { this.voteCount = voteCount; }
    public Double getVoteAverage() { return voteAverage; }
    public void setVoteAverage(Double voteAverage) { this.voteAverage = voteAverage; }
}
