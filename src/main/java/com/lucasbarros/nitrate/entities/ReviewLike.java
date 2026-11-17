package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** Alguém curtiu a review de outra pessoa. (O `liked` da Review é a pessoa curtindo o filme.) */
@Entity
@Table(name = "tb_review_like",
        uniqueConstraints = @UniqueConstraint(columnNames = {"review_id", "user_id"}))
public class ReviewLike implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "review_id")
    private Review review;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    private Instant createdAt = Instant.now();

    public ReviewLike() {
    }

    public ReviewLike(Review review, User user) {
        this.review = review;
        this.user = user;
    }

    public Long getId() { return id; }
    public Review getReview() { return review; }
    public User getUser() { return user; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReviewLike)) return false;
        return Objects.equals(id, ((ReviewLike) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
