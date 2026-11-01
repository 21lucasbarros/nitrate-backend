package com.lucasbarros.nitrate.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

// um filme na filmografia de uma pessoa (como elenco ou equipe)
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbPersonCreditDTO {
    private Long id;
    private String title;
    private String character; // só no elenco
    private String job;       // só na equipe (Director, Writer...)
    private Integer order;    // só no elenco: posição nos créditos

    @JsonProperty("release_date")
    private String releaseDate;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("vote_count")
    private Integer voteCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCharacter() { return character; }
    public void setCharacter(String character) { this.character = character; }
    public String getJob() { return job; }
    public void setJob(String job) { this.job = job; }
    public Integer getOrder() { return order; }
    public void setOrder(Integer order) { this.order = order; }
    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
    public int getVoteCount() { return voteCount == null ? 0 : voteCount; }
    public void setVoteCount(Integer voteCount) { this.voteCount = voteCount; }
}
