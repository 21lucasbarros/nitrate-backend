package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.User;

import java.time.Instant;
import java.util.List;

public class UserProfileDTO {
    private String username;
    private String name;
    private String avatar; // caminho da foto na API; nulo sem foto
    private Instant createdAt;
    private String bio;
    private String twitter;
    private String instagram;
    private long filmsCount;
    private long reviewsCount;
    private long likesCount;
    private long watchlistCount;
    private long followersCount;
    private long followingCount;
    private List<MovieDTO> favorites;
    // se quem está vendo segue este perfil; nulo para visitantes e no próprio perfil
    private Boolean followedByMe;
    // se este perfil segue quem está vendo ("Segue você"); nulo para visitantes e no próprio perfil
    private Boolean followsMe;
    // selos concedidos pelo admin, na ordem do catálogo
    private List<BadgeDTO> badges;
    // se a conta pode abrir o painel de admin; só vem no próprio perfil (/users/me), nulo nos outros
    private Boolean admin;

    public UserProfileDTO(User user, long filmsCount, long reviewsCount, long likesCount, long watchlistCount,
                          long followersCount, long followingCount, List<MovieDTO> favorites, Boolean followedByMe,
                          Boolean followsMe, List<BadgeDTO> badges, Boolean admin) {
        this.username = user.getUsername();
        this.name = user.getName();
        this.avatar = user.getAvatarPath();
        this.createdAt = user.getCreatedAt();
        this.bio = user.getBio();
        this.twitter = user.getTwitter();
        this.instagram = user.getInstagram();
        this.filmsCount = filmsCount;
        this.reviewsCount = reviewsCount;
        this.likesCount = likesCount;
        this.watchlistCount = watchlistCount;
        this.followersCount = followersCount;
        this.followingCount = followingCount;
        this.favorites = favorites;
        this.followedByMe = followedByMe;
        this.followsMe = followsMe;
        this.badges = badges;
        this.admin = admin;
    }

    public String getUsername() { return username; }
    public String getName() { return name; }
    public String getAvatar() { return avatar; }
    public Instant getCreatedAt() { return createdAt; }
    public String getBio() { return bio; }
    public String getTwitter() { return twitter; }
    public String getInstagram() { return instagram; }
    public long getFilmsCount() { return filmsCount; }
    public long getReviewsCount() { return reviewsCount; }
    public long getLikesCount() { return likesCount; }
    public long getWatchlistCount() { return watchlistCount; }
    public long getFollowersCount() { return followersCount; }
    public long getFollowingCount() { return followingCount; }
    public List<MovieDTO> getFavorites() { return favorites; }
    public Boolean getFollowedByMe() { return followedByMe; }
    public Boolean getFollowsMe() { return followsMe; }
    public List<BadgeDTO> getBadges() { return badges; }
    public Boolean getAdmin() { return admin; }
}
