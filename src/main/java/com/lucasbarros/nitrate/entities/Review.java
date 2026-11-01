package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "tb_review",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "movie_id"}))
public class Review implements Serializable {
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

    // de 0.5 a 5, em passos de 0.5. null = assistiu mas não deu nota
    private Double rating;

    @Column(length = 5000)
    private String text;

    private boolean liked;

    // o default no banco preenche as reviews que já existiam quando a coluna foi criada
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean spoiler;

    private LocalDate watchedOn;

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    public Review() {
    }

    public Review(User user, Movie movie) {
        this.user = user;
        this.movie = movie;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Movie getMovie() { return movie; }
    public void setMovie(Movie movie) { this.movie = movie; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }

    public boolean isSpoiler() { return spoiler; }
    public void setSpoiler(boolean spoiler) { this.spoiler = spoiler; }

    public LocalDate getWatchedOn() { return watchedOn; }
    public void setWatchedOn(LocalDate watchedOn) { this.watchedOn = watchedOn; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Review)) return false;
        return Objects.equals(id, ((Review) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
