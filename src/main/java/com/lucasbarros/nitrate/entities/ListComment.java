package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** Comentário na lista de alguém. */
@Entity
@Table(name = "tb_list_comment",
        indexes = @Index(columnList = "list_id, createdAt"))
public class ListComment implements Serializable {
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

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    private Instant createdAt = Instant.now();

    public ListComment() {
    }

    public ListComment(MovieList list, User user, String text) {
        this.list = list;
        this.user = user;
        this.text = text;
    }

    public Long getId() { return id; }
    public MovieList getList() { return list; }
    public User getUser() { return user; }
    public String getText() { return text; }
    public Instant getCreatedAt() { return createdAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ListComment)) return false;
        return Objects.equals(id, ((ListComment) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
