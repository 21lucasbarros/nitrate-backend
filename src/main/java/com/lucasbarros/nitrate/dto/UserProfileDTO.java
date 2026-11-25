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
    // séries: quantas a pessoa registrou (sem repetir) e quantas estão na watchlist de séries
    private long seriesCount;
    private long seriesWatchlistCount;
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
    // e-mail da conta: só no próprio perfil (/users/me), para a tela de configurações; nulo nos outros
    private String email;
    // fundo escolhido para o topo do perfil; nulo = o front usa a cena do primeiro favorito
    private Cover cover;

    // x e y: o enquadramento, de 0 a 100; nulos = o padrão do front
    public record Cover(String path, Long tmdbId, String title, Integer year, Integer x, Integer y) {}

    public UserProfileDTO(User user, long filmsCount, long reviewsCount, long likesCount, long watchlistCount,
                          long followersCount, long followingCount, List<MovieDTO> favorites, Boolean followedByMe,
                          Boolean followsMe, List<BadgeDTO> badges, Boolean admin, String email) {
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
        this.email = email;
        this.cover = user.getCoverPath() == null ? null
                : new Cover(user.getCoverPath(), user.getCoverTmdbId(), user.getCoverTitle(), user.getCoverYear(),
                        user.getCoverX(), user.getCoverY());
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
    public String getEmail() { return email; }
    public Cover getCover() { return cover; }
    public long getSeriesCount() { return seriesCount; }
    public long getSeriesWatchlistCount() { return seriesWatchlistCount; }

    public UserProfileDTO comSeries(long seriesCount, long seriesWatchlistCount) {
        this.seriesCount = seriesCount;
        this.seriesWatchlistCount = seriesWatchlistCount;
        return this;
    }
}
