package com.lucasbarros.nitrate.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbPersonSearchResponseDTO {
    private List<Result> results = new ArrayList<>();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Result {
        private Long id;
        private String name;

        @JsonProperty("profile_path")
        private String profilePath;

        @JsonProperty("known_for_department")
        private String knownForDepartment;

        @JsonProperty("known_for")
        private List<KnownFor> knownFor = new ArrayList<>();

        private Double popularity;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getProfilePath() { return profilePath; }
        public void setProfilePath(String profilePath) { this.profilePath = profilePath; }
        public String getKnownForDepartment() { return knownForDepartment; }
        public void setKnownForDepartment(String knownForDepartment) { this.knownForDepartment = knownForDepartment; }
        public double getPopularity() { return popularity == null ? 0 : popularity; }
        public void setPopularity(Double popularity) { this.popularity = popularity; }
        public List<KnownFor> getKnownFor() { return knownFor; }
        public void setKnownFor(List<KnownFor> knownFor) { this.knownFor = knownFor; }
    }

    // "conhecido por": filmes ou séries (séries vêm com `name` em vez de `title`)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class KnownFor {
        private String title;
        private String name;

        @JsonProperty("media_type")
        private String mediaType;

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getMediaType() { return mediaType; }
        public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    }

    public List<Result> getResults() { return results; }
    public void setResults(List<Result> results) { this.results = results; }
}
