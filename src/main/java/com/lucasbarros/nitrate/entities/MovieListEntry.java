package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

/** Um filme dentro de uma lista, na posição escolhida. */
@Entity
@Table(name = "tb_movie_list_entry",
        uniqueConstraints = @UniqueConstraint(columnNames = {"list_id", "movie_id"}))
public class MovieListEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "list_id")
    private MovieList list;

    @ManyToOne(optional = false)
    @JoinColumn(name = "movie_id")
    private Movie movie;

    /** 1 = primeiro da lista */
    @Column(nullable = false)
    private int position;

    public MovieListEntry() {
    }

    public MovieListEntry(MovieList list, Movie movie, int position) {
        this.list = list;
        this.movie = movie;
        this.position = position;
    }

    public Long getId() { return id; }
    public MovieList getList() { return list; }
    public Movie getMovie() { return movie; }
    public int getPosition() { return position; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MovieListEntry)) return false;
        return Objects.equals(id, ((MovieListEntry) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
