package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** Um filme ou uma série dentro de uma lista, na posição escolhida (só um dos dois vem preenchido). */
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

    // nulo quando o item é uma série
    @ManyToOne
    @JoinColumn(name = "movie_id")
    private Movie movie;

    // nulo quando o item é um filme
    @ManyToOne
    @JoinColumn(name = "series_id")
    private Series series;

    /** 1 = primeiro da lista */
    @Column(nullable = false)
    private int position;

    /** quando o filme entrou na lista; null nas entradas de antes deste campo existir */
    private Instant addedAt = Instant.now();

    public MovieListEntry() {
    }

    public MovieListEntry(MovieList list, Movie movie, int position) {
        this.list = list;
        this.movie = movie;
        this.position = position;
    }

    public MovieListEntry(MovieList list, Series series, int position) {
        this.list = list;
        this.series = series;
        this.position = position;
    }

    public Long getId() { return id; }
    public MovieList getList() { return list; }
    public Movie getMovie() { return movie; }
    public Series getSeries() { return series; }
    public boolean isSeries() { return series != null; }
    /** "M12" ou "S3": identifica o item entre filmes e séries (os ids das duas tabelas se repetem) */
    public String chave() { return series != null ? "S" + series.getId() : "M" + movie.getId(); }
    public int getPosition() { return position; }
    public Instant getAddedAt() { return addedAt; }
    public void setAddedAt(Instant addedAt) { this.addedAt = addedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MovieListEntry)) return false;
        return Objects.equals(id, ((MovieListEntry) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
