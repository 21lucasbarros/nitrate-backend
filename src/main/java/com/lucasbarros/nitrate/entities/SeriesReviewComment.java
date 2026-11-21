package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** Comentário no registro de série de alguém. */
@Entity
@Table(name = "tb_series_review_comment", indexes = @Index(columnList = "review_id, createdAt"))
public class SeriesReviewComment implements Serializable {
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

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    private Instant createdAt = Instant.now();

    public SeriesReviewComment() {
    }

    public SeriesReviewComment(SeriesReview review, User user, String text) {
        this.review = review;
        this.user = user;
        this.text = text;
    }

    public Long getId() { return id; }
    public SeriesReview getReview() { return review; }
    public User getUser() { return user; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SeriesReviewComment)) return false;
        return Objects.equals(id, ((SeriesReviewComment) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
