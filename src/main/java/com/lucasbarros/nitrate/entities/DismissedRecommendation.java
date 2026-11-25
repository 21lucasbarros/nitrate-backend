package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;

/** "não tenho interesse" numa recomendação: o filme some da home e pesa contra os parecidos com ele */
@Entity
@Table(name = "tb_dismissed_recommendation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "movie_id"}))
public class DismissedRecommendation implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    private Instant createdAt = Instant.now();

    public DismissedRecommendation() {}

    public DismissedRecommendation(User user, Movie movie) {
        this.user = user;
        this.movie = movie;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Movie getMovie() { return movie; }
    public Instant getCreatedAt() { return createdAt; }
}
