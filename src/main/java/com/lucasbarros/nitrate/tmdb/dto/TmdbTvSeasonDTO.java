package com.lucasbarros.nitrate.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/** /tv/{id}/season/{n}: a temporada com os episódios */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbTvSeasonDTO {
    private String name;
    private String overview;

    @JsonProperty("season_number")
    private Integer seasonNumber;

    @JsonProperty("air_date")
    private String airDate;

    @JsonProperty("poster_path")
    private String posterPath;

    private List<Episode> episodes = new ArrayList<>();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Episode {
        @JsonProperty("episode_number")
        private Integer episodeNumber;
        private String name;
        private String overview;
        @JsonProperty("air_date")
        private String airDate;
        private Integer runtime;
        @JsonProperty("still_path")
        private String stillPath;
        @JsonProperty("vote_average")
        private Double voteAverage;

        public Integer getEpisodeNumber() { return episodeNumber; }
        public void setEpisodeNumber(Integer episodeNumber) { this.episodeNumber = episodeNumber; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getOverview() { return overview; }
        public void setOverview(String overview) { this.overview = overview; }
        public String getAirDate() { return airDate; }
        public void setAirDate(String airDate) { this.airDate = airDate; }
        public Integer getRuntime() { return runtime; }
        public void setRuntime(Integer runtime) { this.runtime = runtime; }
        public String getStillPath() { return stillPath; }
        public void setStillPath(String stillPath) { this.stillPath = stillPath; }
        public Double getVoteAverage() { return voteAverage; }
        public void setVoteAverage(Double voteAverage) { this.voteAverage = voteAverage; }
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    public Integer getSeasonNumber() { return seasonNumber; }
    public void setSeasonNumber(Integer seasonNumber) { this.seasonNumber = seasonNumber; }
    public String getAirDate() { return airDate; }
    public void setAirDate(String airDate) { this.airDate = airDate; }
    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
    public List<Episode> getEpisodes() { return episodes; }
    public void setEpisodes(List<Episode> episodes) { this.episodes = episodes; }
}
