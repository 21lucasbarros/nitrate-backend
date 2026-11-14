package com.lucasbarros.nitrate.tmdb.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** /movie/{id}/watch/providers: onde assistir, por país (dados da JustWatch). */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TmdbWatchProvidersDTO {
    private Map<String, Country> results = new HashMap<>();

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Country {
        private String link; // página do TMDB com os links para cada serviço
        private List<Provider> flatrate = new ArrayList<>();
        private List<Provider> rent = new ArrayList<>();
        private List<Provider> buy = new ArrayList<>();
        private List<Provider> free = new ArrayList<>();
        private List<Provider> ads = new ArrayList<>();

        public String getLink() { return link; }
        public void setLink(String link) { this.link = link; }
        public List<Provider> getFlatrate() { return flatrate; }
        public void setFlatrate(List<Provider> flatrate) { this.flatrate = flatrate; }
        public List<Provider> getRent() { return rent; }
        public void setRent(List<Provider> rent) { this.rent = rent; }
        public List<Provider> getBuy() { return buy; }
        public void setBuy(List<Provider> buy) { this.buy = buy; }
        public List<Provider> getFree() { return free; }
        public void setFree(List<Provider> free) { this.free = free; }
        public List<Provider> getAds() { return ads; }
        public void setAds(List<Provider> ads) { this.ads = ads; }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Provider {
        @JsonProperty("provider_id")
        private Long providerId;

        @JsonProperty("provider_name")
        private String providerName;

        @JsonProperty("logo_path")
        private String logoPath;

        @JsonProperty("display_priority")
        private Integer displayPriority;

        public Long getProviderId() { return providerId; }
        public void setProviderId(Long providerId) { this.providerId = providerId; }
        public String getProviderName() { return providerName; }
        public void setProviderName(String providerName) { this.providerName = providerName; }
        public String getLogoPath() { return logoPath; }
        public void setLogoPath(String logoPath) { this.logoPath = logoPath; }
        public int getDisplayPriority() { return displayPriority == null ? Integer.MAX_VALUE : displayPriority; }
        public void setDisplayPriority(Integer displayPriority) { this.displayPriority = displayPriority; }
    }

    public Map<String, Country> getResults() { return results; }
    public void setResults(Map<String, Country> results) { this.results = results; }
}
