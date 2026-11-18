package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * Um dos atores/atrizes ou diretores favoritos do perfil (até 5 de cada, em ordem).
 * Pessoas não ficam no banco: guardamos o id do TMDB e uma cópia do nome e da foto,
 * para o perfil não depender do TMDB a cada visita.
 */
@Entity
@Table(name = "tb_favorite_person",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "kind", "tmdb_id"}))
public class FavoritePerson implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Kind { ACTOR, DIRECTOR }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Kind kind;

    @Column(name = "tmdb_id", nullable = false)
    private Long tmdbId;

    @Column(nullable = false)
    private String name;

    private String profilePath;

    /** 1 = o favorito de todos */
    @Column(nullable = false)
    private int position;

    public FavoritePerson() {
    }

    public FavoritePerson(User user, Kind kind, Long tmdbId, String name, String profilePath, int position) {
        this.user = user;
        this.kind = kind;
        this.tmdbId = tmdbId;
        this.name = name;
        this.profilePath = profilePath;
        this.position = position;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Kind getKind() { return kind; }
    public Long getTmdbId() { return tmdbId; }
    public String getName() { return name; }
    public String getProfilePath() { return profilePath; }
    public int getPosition() { return position; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FavoritePerson)) return false;
        return Objects.equals(id, ((FavoritePerson) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
