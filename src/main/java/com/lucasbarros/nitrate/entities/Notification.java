package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Aviso para a pessoa: um filme da watchlist chegou aos cinemas ou ao streaming, uma série
 * estreou ou ganhou temporada nova (Radar de Estreias), alguém começou a seguir ela, ou alguém
 * marcou que assistiu um filme ou uma série com ela (convite de review em conjunto) e aceitou o convite dela.
 */
@Entity
@Table(name = "tb_notification",
        // cada pessoa é avisada uma vez só de cada estreia de cada filme (nas séries quem
        // garante é o SeriesRadarService, já que movie_id fica nulo)
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "movie_id", "type"}),
        indexes = @Index(columnList = "user_id, createdAt"))
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type { CINEMA, STREAMING, FOLLOW, SERIES_PREMIERE, SERIES_SEASON, COLLAB_INVITE, COLLAB_ACCEPTED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    // nas estreias de filmes; nulo nos outros tipos
    @ManyToOne
    @JoinColumn(name = "movie_id")
    private Movie movie;

    // em SERIES_PREMIERE e SERIES_SEASON; nulo nos outros tipos
    @ManyToOne
    @JoinColumn(name = "series_id")
    private Series series;

    // em SERIES_SEASON, a temporada que estreou
    private Integer seasonNumber;

    // em COLLAB_INVITE e COLLAB_ACCEPTED; o filme vem da review de quem convidou (movie_id fica
    // nulo para não esbarrar na unicidade de (user, movie, type) ao assistir junto de novo)
    @ManyToOne
    @JoinColumn(name = "collaboration_id")
    private ReviewCollaboration collaboration;

    // em COLLAB_INVITE e COLLAB_ACCEPTED de um registro de série (no lugar de `collaboration`)
    @ManyToOne
    @JoinColumn(name = "series_collaboration_id")
    private SeriesReviewCollaboration seriesCollaboration;

    // quem fez a ação (em FOLLOW, quem começou a seguir; nos convites, quem convidou ou aceitou); nulo nas estreias
    @ManyToOne
    @JoinColumn(name = "actor_id")
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Type type;

    // complemento do aviso: a data de estreia ou os serviços de streaming
    @Column(length = 200)
    private String detail;

    private Instant createdAt = Instant.now();

    // nulo = ainda não lida
    private Instant readAt;

    public Notification() {
    }

    public Notification(User user, Movie movie, Type type, String detail) {
        this.user = user;
        this.movie = movie;
        this.type = type;
        this.detail = detail;
    }

    public static Notification estreiaDeSerie(User user, Series series, String detail) {
        Notification n = new Notification();
        n.user = user;
        n.series = series;
        n.type = Type.SERIES_PREMIERE;
        n.detail = detail;
        return n;
    }

    public static Notification novaTemporada(User user, Series series, int seasonNumber, String detail) {
        Notification n = estreiaDeSerie(user, series, detail);
        n.type = Type.SERIES_SEASON;
        n.seasonNumber = seasonNumber;
        return n;
    }

    public static Notification follow(User seguido, User seguidor) {
        Notification n = new Notification();
        n.user = seguido;
        n.actor = seguidor;
        n.type = Type.FOLLOW;
        return n;
    }

    public static Notification conviteDeReview(ReviewCollaboration c) {
        Notification n = new Notification();
        n.user = c.getInvitee();
        n.actor = c.getAuthor();
        n.collaboration = c;
        n.type = Type.COLLAB_INVITE;
        return n;
    }

    public static Notification conviteAceito(ReviewCollaboration c) {
        Notification n = new Notification();
        n.user = c.getAuthor();
        n.actor = c.getInvitee();
        n.collaboration = c;
        n.type = Type.COLLAB_ACCEPTED;
        return n;
    }

    public static Notification conviteDeReview(SeriesReviewCollaboration c) {
        Notification n = new Notification();
        n.user = c.getInvitee();
        n.actor = c.getAuthor();
        n.seriesCollaboration = c;
        n.type = Type.COLLAB_INVITE;
        return n;
    }

    public static Notification conviteAceito(SeriesReviewCollaboration c) {
        Notification n = new Notification();
        n.user = c.getAuthor();
        n.actor = c.getInvitee();
        n.seriesCollaboration = c;
        n.type = Type.COLLAB_ACCEPTED;
        return n;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Movie getMovie() { return movie; }
    public Series getSeries() { return series; }
    public Integer getSeasonNumber() { return seasonNumber; }
    public ReviewCollaboration getCollaboration() { return collaboration; }
    public SeriesReviewCollaboration getSeriesCollaboration() { return seriesCollaboration; }
    public User getActor() { return actor; }
    public Type getType() { return type; }
    public String getDetail() { return detail; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }
    public void setReadAt(Instant readAt) { this.readAt = readAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification)) return false;
        return Objects.equals(id, ((Notification) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
