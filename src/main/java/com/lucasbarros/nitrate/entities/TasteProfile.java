package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Respostas do questionário de gostos do onboarding (ver TasteQuiz). Os ids são os do questionário,
 * guardados como texto; `seedMovieIds` são os filmes que a pessoa marcou como "já vi e gostei" e
 * servem de sementes para as recomendações até ela curtir filmes de verdade no diário.
 * Não viram registros: o diário só tem o que a pessoa registrou.
 */
@Entity
@Table(name = "tb_taste_profile")
public class TasteProfile implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private Long id;

    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // EAGER: são poucos itens, lidos fora de transação pelo RecommendationService
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_taste_genre", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "genre", length = 40)
    private Set<String> genres = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_taste_answer", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "answer", length = 40)
    private Set<String> answers = new HashSet<>();

    // a época saiu do questionário e não é mais lida: fica mapeada só para o Hibernate apagar as
    // linhas antigas junto com o perfil (a tabela tem chave estrangeira para ele)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_taste_era", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "era", length = 40)
    private Set<String> eras = new HashSet<>();

    // na ordem em que foram marcados: as primeiras viram sementes antes
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "tb_taste_seed", joinColumns = @JoinColumn(name = "user_id"))
    @OrderColumn(name = "position")
    @Column(name = "movie_id")
    private List<Long> seedMovieIds = new ArrayList<>();

    private Instant updatedAt = Instant.now();

    public TasteProfile() {
    }

    public TasteProfile(User user) {
        this.user = user;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }

    public Set<String> getGenres() { return genres; }
    public Set<String> getAnswers() { return answers; }
    public Set<String> getEras() { return eras; }
    public List<Long> getSeedMovieIds() { return seedMovieIds; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    /** respondeu alguma coisa além de pular tudo */
    public boolean temPreferencias() {
        return !genres.isEmpty() || !answers.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TasteProfile)) return false;
        return Objects.equals(id, ((TasteProfile) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
