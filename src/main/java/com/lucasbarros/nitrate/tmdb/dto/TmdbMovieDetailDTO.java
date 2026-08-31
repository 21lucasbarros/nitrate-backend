package com.lucasbarros.nitrate.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbMovieDetailDTO {
    private Long id;
    private String title;
    private String overview;

    @JsonProperty("release_date")
    private String releaseDate;

    @JsonProperty("vote_average")
    private double voteAverage;

    @JsonProperty("poster_path")
    private String posterPath;

    private List<TmdbGenreDTO> genres = new ArrayList<>();

    @JsonProperty("belongs_to_collection")
    private TmdbCollectionDTO belongsToCollection;

    private TmdbCreditsDTO credits;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    public String getReleaseDate() { return releaseDate; }
    public void setReleaseDate(String releaseDate) { this.releaseDate = releaseDate; }
    public Double getVoteAverage() { return voteAverage; }
    public void setVoteAverage(Double voteAverage) { this.voteAverage = voteAverage; }
    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
    public List<TmdbGenreDTO> getGenres() { return genres; }
    public void setGenres(List<TmdbGenreDTO> genres) { this.genres = genres; }
    public TmdbCollectionDTO getBelongsToCollection() { return belongsToCollection; }
    public void setBelongsToCollection(TmdbCollectionDTO belongsToCollection) { this.belongsToCollection = belongsToCollection; }
    public TmdbCreditsDTO getCredits() { return credits; }
    public void setCredits(TmdbCreditsDTO credits) { this.credits = credits; }
}
