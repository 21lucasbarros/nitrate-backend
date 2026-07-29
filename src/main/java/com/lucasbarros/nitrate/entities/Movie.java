package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.security.PrivateKey;
import java.util.*;

@Entity
@Table(name = "tb_movie")
public class Movie implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    private Integer releaseYear;

    private Double rating;

    @ManyToOne
    @JoinColumn(name = "director_id")
    private Director director;

    @ManyToOne
    @JoinColumn(name = "franchise_id")
    private Franchise franchise;

    @ManyToMany
    @JoinTable(name = "tb_movie_genre",
            joinColumns = @JoinColumn(name = "movie_id"),
            inverseJoinColumns = @JoinColumn(name = "genre_id"))
    private Set<Genre> genres = new HashSet<>();

    @OneToMany(mappedBy = "movie", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MovieCast> cast = new ArrayList<>();

    public Movie() {
    }

    public Movie(String title, Integer releaseYear, Double rating, Director director, Franchise franchise) {
        this.title = title;
        this.releaseYear = releaseYear;
        this.rating = rating;
        this.director = director;
        this.franchise = franchise;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getReleaseYear() {
        return releaseYear;
    }

    public void setReleaseYear(Integer releaseYear) {
        this.releaseYear = releaseYear;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public Director getDirector() {
        return director;
    }

    public void setDirector(Director director) {
        this.director = director;
    }

    public Franchise getFranchise() {
        return franchise;
    }

    public void setFranchise(Franchise franchise) {
        this.franchise = franchise;
    }

    public Set<Genre> getGenres() {
        return genres;
    }

    public void setGenres(Set<Genre> genres) {
        this.genres = genres;
    }

    public List<MovieCast> getCast() {
        return cast;
    }

    public void setCast(List<MovieCast> cast) {
        this.cast = cast;
    }

    public int getEra() {
        return (releaseYear / 10) * 10;
    }

    public MovieCast findCastMember(String actorName) {
        for(MovieCast membro : cast) {
            if(membro.getActor().getName().equalsIgnoreCase(actorName)) {
                return membro;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return title + " (" + releaseYear + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Movie)) return false;
        Movie other = (Movie) o;
        return Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
