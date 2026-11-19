package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** Alguém curtiu a lista de outra pessoa. */
@Entity
@Table(name = "tb_list_like",
        uniqueConstraints = @UniqueConstraint(columnNames = {"list_id", "user_id"}))
public class ListLike implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "list_id")
    private MovieList list;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    private Instant createdAt = Instant.now();

    public ListLike() {
    }

    public ListLike(MovieList list, User user) {
        this.list = list;
        this.user = user;
    }

    public Long getId() { return id; }
    public MovieList getList() { return list; }
    public User getUser() { return user; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ListLike)) return false;
        return Objects.equals(id, ((ListLike) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
