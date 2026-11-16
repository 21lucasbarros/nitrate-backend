package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** Aviso do Radar de Estreias: um filme da watchlist chegou aos cinemas ou ao streaming. */
@Entity
@Table(name = "tb_notification",
        // cada pessoa é avisada uma vez só de cada estreia de cada filme
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "movie_id", "type"}),
        indexes = @Index(columnList = "user_id, createdAt"))
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type { CINEMA, STREAMING }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "movie_id")
    private Movie movie;

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

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Movie getMovie() { return movie; }
    public Type getType() { return type; }
    public String getDetail() { return detail; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getReadAt() { return readAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Notification)) return false;
        return Objects.equals(id, ((Notification) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
