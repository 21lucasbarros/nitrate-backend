package com.lucasbarros.nitrate.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbCastMemberDTO {
    private Long id;
    private String name;
    private String character;
    @JsonProperty("profile_path")
    private String profilePath;
    private Integer order;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getProfilePath() { return profilePath; }
    public void setProfilePath(String profilePath) { this.profilePath = profilePath; }
    public String getCharacter() { return character; }
    public void setCharacter(String character) { this.character = character; }
}
