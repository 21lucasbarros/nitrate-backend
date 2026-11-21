package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Conquista do perfil, ganha assistindo filmes (ao contrário dos selos, que o admin concede).
 * Hoje só existe a filmografia completa de uma pessoa; guardamos uma cópia do nome e da foto
 * (como nos favoritos) para a aba de conquistas não depender do TMDB. A conquista fica mesmo que
 * a pessoa lance filmes novos depois: `films` é quantos eram quando foi conquistada.
 */
@Entity
@Table(name = "tb_user_achievement",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "type", "person_tmdb_id"}))
public class UserAchievement implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String FILMOGRAFIA_COMPLETA = "FILMOGRAFIA_COMPLETA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    // texto livre, e não enum, pelo mesmo motivo do tb_user_badge: tipos novos sem mexer no banco
    @Column(nullable = false, length = 40)
    private String type;

    @Column(name = "person_tmdb_id", nullable = false)
    private Long personTmdbId;

    @Column(nullable = false)
    private String personName;

    private String personProfilePath;

    @Column(nullable = false)
    private int films;

    // a pessoa pode tirar a conquista do perfil; para quem visita, ela some
    @Column(nullable = false)
    private boolean hidden = false;

    private Instant achievedAt = Instant.now();

    public UserAchievement() {
    }

    public UserAchievement(User user, String type, Long personTmdbId, String personName, String personProfilePath, int films) {
        this.user = user;
        this.type = type;
        this.personTmdbId = personTmdbId;
        this.personName = personName;
        this.personProfilePath = personProfilePath;
        this.films = films;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getType() { return type; }
    public Long getPersonTmdbId() { return personTmdbId; }
    public String getPersonName() { return personName; }
    public String getPersonProfilePath() { return personProfilePath; }
    public int getFilms() { return films; }
    public boolean isHidden() { return hidden; }
    public void setHidden(boolean hidden) { this.hidden = hidden; }
    public Instant getAchievedAt() { return achievedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserAchievement)) return false;
        return Objects.equals(id, ((UserAchievement) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
