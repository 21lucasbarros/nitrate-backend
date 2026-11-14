package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** `follower` segue `followed`. Seguir não precisa de aprovação: os diários são públicos. */
@Entity
@Table(name = "tb_follow",
        uniqueConstraints = @UniqueConstraint(columnNames = {"follower_id", "followed_id"}),
        indexes = @Index(columnList = "followed_id"))
public class Follow implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "follower_id")
    private User follower;

    @ManyToOne(optional = false)
    @JoinColumn(name = "followed_id")
    private User followed;

    private Instant createdAt = Instant.now();

    public Follow() {
    }

    public Follow(User follower, User followed) {
        this.follower = follower;
        this.followed = followed;
    }

    public Long getId() { return id; }
    public User getFollower() { return follower; }
    public User getFollowed() { return followed; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Follow)) return false;
        return Objects.equals(id, ((Follow) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
