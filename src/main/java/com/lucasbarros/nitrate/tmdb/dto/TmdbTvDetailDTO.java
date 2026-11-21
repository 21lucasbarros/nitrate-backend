package com.lucasbarros.nitrate.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

/** /tv/{id}, com o elenco de todas as temporadas (append_to_response=aggregate_credits) */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbTvDetailDTO {
    private Long id;
    private String name;
    private String overview;
    private String tagline;
    private String status;

    @JsonProperty("first_air_date")
    private String firstAirDate;

    @JsonProperty("last_air_date")
    private String lastAirDate;

    @JsonProperty("in_production")
    private boolean inProduction;

    @JsonProperty("poster_path")
    private String posterPath;

    @JsonProperty("backdrop_path")
    private String backdropPath;

    @JsonProperty("vote_average")
    private Double voteAverage;

    @JsonProperty("number_of_seasons")
    private Integer numberOfSeasons;

    @JsonProperty("number_of_episodes")
    private Integer numberOfEpisodes;

    @JsonProperty("episode_run_time")
    private List<Integer> episodeRunTime = new ArrayList<>();

    private List<TmdbGenreDTO> genres = new ArrayList<>();

    @JsonProperty("created_by")
    private List<Creator> createdBy = new ArrayList<>();

    private List<Network> networks = new ArrayList<>();

    private List<Season> seasons = new ArrayList<>();

    @JsonProperty("next_episode_to_air")
    private Episode nextEpisodeToAir;

    @JsonProperty("aggregate_credits")
    private AggregateCredits aggregateCredits;

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Creator {
        private Long id;
        private String name;
        @JsonProperty("profile_path")
        private String profilePath;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getProfilePath() { return profilePath; }
        public void setProfilePath(String profilePath) { this.profilePath = profilePath; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Network {
        private Long id;
        private String name;
        @JsonProperty("logo_path")
        private String logoPath;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLogoPath() { return logoPath; }
        public void setLogoPath(String logoPath) { this.logoPath = logoPath; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Season {
        private String name;
        private String overview;
        @JsonProperty("season_number")
        private Integer seasonNumber;
        @JsonProperty("episode_count")
        private Integer episodeCount;
        @JsonProperty("air_date")
        private String airDate;
        @JsonProperty("poster_path")
        private String posterPath;
        @JsonProperty("vote_average")
        private Double voteAverage;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getOverview() { return overview; }
        public void setOverview(String overview) { this.overview = overview; }
        public Integer getSeasonNumber() { return seasonNumber; }
        public void setSeasonNumber(Integer seasonNumber) { this.seasonNumber = seasonNumber; }
        public Integer getEpisodeCount() { return episodeCount; }
        public void setEpisodeCount(Integer episodeCount) { this.episodeCount = episodeCount; }
        public String getAirDate() { return airDate; }
        public void setAirDate(String airDate) { this.airDate = airDate; }
        public String getPosterPath() { return posterPath; }
        public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
        public Double getVoteAverage() { return voteAverage; }
        public void setVoteAverage(Double voteAverage) { this.voteAverage = voteAverage; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Episode {
        private String name;
        @JsonProperty("air_date")
        private String airDate;
        @JsonProperty("season_number")
        private Integer seasonNumber;
        @JsonProperty("episode_number")
        private Integer episodeNumber;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getAirDate() { return airDate; }
        public void setAirDate(String airDate) { this.airDate = airDate; }
        public Integer getSeasonNumber() { return seasonNumber; }
        public void setSeasonNumber(Integer seasonNumber) { this.seasonNumber = seasonNumber; }
        public Integer getEpisodeNumber() { return episodeNumber; }
        public void setEpisodeNumber(Integer episodeNumber) { this.episodeNumber = episodeNumber; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AggregateCredits {
        private List<CastMember> cast = new ArrayList<>();

        public List<CastMember> getCast() { return cast; }
        public void setCast(List<CastMember> cast) { this.cast = cast; }
    }

    /** no elenco agregado, cada pessoa traz os papéis que fez e em quantos episódios */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CastMember {
        private Long id;
        private String name;
        @JsonProperty("profile_path")
        private String profilePath;
        private Integer order;
        @JsonProperty("total_episode_count")
        private Integer totalEpisodeCount;
        private List<Role> roles = new ArrayList<>();

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getProfilePath() { return profilePath; }
        public void setProfilePath(String profilePath) { this.profilePath = profilePath; }
        public Integer getOrder() { return order; }
        public void setOrder(Integer order) { this.order = order; }
        public Integer getTotalEpisodeCount() { return totalEpisodeCount; }
        public void setTotalEpisodeCount(Integer totalEpisodeCount) { this.totalEpisodeCount = totalEpisodeCount; }
        public List<Role> getRoles() { return roles; }
        public void setRoles(List<Role> roles) { this.roles = roles; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Role {
        private String character;
        @JsonProperty("episode_count")
        private Integer episodeCount;

        public String getCharacter() { return character; }
        public void setCharacter(String character) { this.character = character; }
        public Integer getEpisodeCount() { return episodeCount; }
        public void setEpisodeCount(Integer episodeCount) { this.episodeCount = episodeCount; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }
    public String getTagline() { return tagline; }
    public void setTagline(String tagline) { this.tagline = tagline; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFirstAirDate() { return firstAirDate; }
    public void setFirstAirDate(String firstAirDate) { this.firstAirDate = firstAirDate; }
    public String getLastAirDate() { return lastAirDate; }
    public void setLastAirDate(String lastAirDate) { this.lastAirDate = lastAirDate; }
    public boolean isInProduction() { return inProduction; }
    public void setInProduction(boolean inProduction) { this.inProduction = inProduction; }
    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }
    public String getBackdropPath() { return backdropPath; }
    public void setBackdropPath(String backdropPath) { this.backdropPath = backdropPath; }
    public Double getVoteAverage() { return voteAverage; }
    public void setVoteAverage(Double voteAverage) { this.voteAverage = voteAverage; }
    public Integer getNumberOfSeasons() { return numberOfSeasons; }
    public void setNumberOfSeasons(Integer numberOfSeasons) { this.numberOfSeasons = numberOfSeasons; }
    public Integer getNumberOfEpisodes() { return numberOfEpisodes; }
    public void setNumberOfEpisodes(Integer numberOfEpisodes) { this.numberOfEpisodes = numberOfEpisodes; }
    public List<Integer> getEpisodeRunTime() { return episodeRunTime; }
    public void setEpisodeRunTime(List<Integer> episodeRunTime) { this.episodeRunTime = episodeRunTime; }
    public List<TmdbGenreDTO> getGenres() { return genres; }
    public void setGenres(List<TmdbGenreDTO> genres) { this.genres = genres; }
    public List<Creator> getCreatedBy() { return createdBy; }
    public void setCreatedBy(List<Creator> createdBy) { this.createdBy = createdBy; }
    public List<Network> getNetworks() { return networks; }
    public void setNetworks(List<Network> networks) { this.networks = networks; }
    public List<Season> getSeasons() { return seasons; }
    public void setSeasons(List<Season> seasons) { this.seasons = seasons; }
    public Episode getNextEpisodeToAir() { return nextEpisodeToAir; }
    public void setNextEpisodeToAir(Episode nextEpisodeToAir) { this.nextEpisodeToAir = nextEpisodeToAir; }
    public AggregateCredits getAggregateCredits() { return aggregateCredits; }
    public void setAggregateCredits(AggregateCredits aggregateCredits) { this.aggregateCredits = aggregateCredits; }
}
