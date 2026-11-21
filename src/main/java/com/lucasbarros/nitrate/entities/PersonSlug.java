package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * Endereço legível de uma pessoa do TMDB ("/person/ben-stiller"). Pessoas não ficam no banco,
 * mas o slug precisa ficar: homônimos ganham sufixo ("-2") e o endereço não pode mudar depois.
 */
@Entity
@Table(name = "tb_person_slug")
public class PersonSlug implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @Column(name = "tmdb_id")
    private Long tmdbId;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    public PersonSlug() {
    }

    public PersonSlug(Long tmdbId, String slug) {
        this.tmdbId = tmdbId;
        this.slug = slug;
    }

    public Long getTmdbId() { return tmdbId; }
    public String getSlug() { return slug; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PersonSlug)) return false;
        return Objects.equals(tmdbId, ((PersonSlug) o).tmdbId);
    }

    @Override
    public int hashCode() { return Objects.hash(tmdbId); }
}
