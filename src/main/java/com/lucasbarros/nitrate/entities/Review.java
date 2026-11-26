package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "tb_review") // várias reviews do mesmo filme = reassistido (ver `rewatch`)
public class Review implements Serializable {
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

    // de 0.5 a 5, em passos de 0.5. null = assistiu mas não deu nota
    private Double rating;

    // TEXT: reviews importadas do Letterboxd passam de 10 mil caracteres (ver SchemaAdjustments)
    @Column(columnDefinition = "text")
    private String text;

    private boolean liked;

    // já tinha assistido antes (no Nitrate ou fora dele)
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean rewatch;

    // o default no banco preenche as reviews que já existiam quando a coluna foi criada
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean spoiler;

    private LocalDate watchedOn;

    // pôster escolhido pela pessoa entre os do TMDB, no lugar do pôster do filme nas reviews dela;
    // nulo = o do filme. Igual em todos os registros dela do mesmo filme (ver ReviewService)
    private String posterPath;

    // review em conjunto, em quem aceitou um convite: o convite que liga esta review à de quem
    // convidou (nula na review de quem convidou, que tem os convites em `invites`)
    @ManyToOne
    @JoinColumn(name = "collaboration_id")
    private ReviewCollaboration collaboration;

    // review em conjunto, em quem convidou: um convite para cada pessoa marcada no "assisti com"
    @OneToMany(mappedBy = "authorReview")
    @OrderBy("id")
    private List<ReviewCollaboration> invites = new ArrayList<>();

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    public Review() {
    }

    public Review(User user, Movie movie) {
        this.user = user;
        this.movie = movie;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Movie getMovie() { return movie; }
    public void setMovie(Movie movie) { this.movie = movie; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }

    public boolean isRewatch() { return rewatch; }
    public void setRewatch(boolean rewatch) { this.rewatch = rewatch; }

    public boolean isSpoiler() { return spoiler; }
    public void setSpoiler(boolean spoiler) { this.spoiler = spoiler; }

    public LocalDate getWatchedOn() { return watchedOn; }
    public void setWatchedOn(LocalDate watchedOn) { this.watchedOn = watchedOn; }

    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }

    public ReviewCollaboration getCollaboration() { return collaboration; }
    public void setCollaboration(ReviewCollaboration collaboration) { this.collaboration = collaboration; }

    public List<ReviewCollaboration> getInvites() { return invites; }

    /** a review de quem convidou, que reúne a sessão em conjunto: esta mesma, se não entrou por convite */
    public Review reviewDeOrigem() {
        return collaboration == null ? this : collaboration.getAuthorReview();
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Review)) return false;
        return Objects.equals(id, ((Review) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
