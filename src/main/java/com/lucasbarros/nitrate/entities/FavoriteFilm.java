package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

/** Um dos filmes favoritos que a pessoa escolhe para o topo do perfil (até 5, em ordem). */
@Entity
@Table(name = "tb_favorite_film",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "movie_id"}))
public class FavoriteFilm implements Serializable {
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

    /** 1 = o favorito de todos */
    @Column(nullable = false)
    private int position;

    public FavoriteFilm() {
    }

    public FavoriteFilm(User user, Movie movie, int position) {
        this.user = user;
        this.movie = movie;
        this.position = position;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public Movie getMovie() { return movie; }
    public int getPosition() { return position; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FavoriteFilm)) return false;
        return Objects.equals(id, ((FavoriteFilm) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
