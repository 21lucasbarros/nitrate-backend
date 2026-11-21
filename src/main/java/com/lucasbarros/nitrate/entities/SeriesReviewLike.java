package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** Alguém curtiu o registro de série de outra pessoa. (O `liked` da SeriesReview é a pessoa curtindo a série.) */
@Entity
@Table(name = "tb_series_review_like",
        uniqueConstraints = @UniqueConstraint(columnNames = {"review_id", "user_id"}))
public class SeriesReviewLike implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "review_id")
    private SeriesReview review;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    private Instant createdAt = Instant.now();

    public SeriesReviewLike() {
    }

    public SeriesReviewLike(SeriesReview review, User user) {
        this.review = review;
        this.user = user;
    }

    public Long getId() { return id; }
    public SeriesReview getReview() { return review; }
    public User getUser() { return user; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SeriesReviewLike)) return false;
        return Objects.equals(id, ((SeriesReviewLike) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
