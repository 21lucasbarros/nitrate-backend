package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Uma lista criada pela pessoa (maratona, retrospectiva de diretor, coleção...). Pública ou privada. */
@Entity
@Table(name = "tb_movie_list")
public class MovieList implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    // privada = só a dona vê
    @Column(nullable = false, columnDefinition = "boolean not null default true")
    private boolean publicList = true;

    // a ordem importa (ranking, ordem da maratona)
    @OneToMany(mappedBy = "list", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position asc")
    private List<MovieListEntry> entries = new ArrayList<>();

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    public MovieList() {
    }

    public MovieList(User user) {
        this.user = user;
    }

    public Long getId() { return id; }

    public User getUser() { return user; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public boolean isPublicList() { return publicList; }
    public void setPublicList(boolean publicList) { this.publicList = publicList; }

    public List<MovieListEntry> getEntries() { return entries; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MovieList)) return false;
        return Objects.equals(id, ((MovieList) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
