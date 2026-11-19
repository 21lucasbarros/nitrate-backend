package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** Alguém salvou a lista de outra pessoa para ver depois. */
@Entity
@Table(name = "tb_list_save",
        uniqueConstraints = @UniqueConstraint(columnNames = {"list_id", "user_id"}))
public class ListSave implements Serializable {
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

    public ListSave() {
    }

    public ListSave(MovieList list, User user) {
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
        if (!(o instanceof ListSave)) return false;
        return Objects.equals(id, ((ListSave) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
