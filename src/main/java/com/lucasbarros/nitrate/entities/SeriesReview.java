package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Registro de uma série no diário: a série inteira (seasonNumber nulo), uma temporada ou um episódio dela. */
@Entity
@Table(name = "tb_series_review")
public class SeriesReview implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "series_id")
    private Series series;

    // nulo = a série como um todo; 0 = especiais, como no TMDB
    private Integer seasonNumber;

    // nulo = a temporada inteira (ou a série); com número, um episódio da temporada em seasonNumber
    private Integer episodeNumber;

    // de 0.5 a 5, em passos de 0.5. null = assistiu mas não deu nota
    private Double rating;

    @Column(columnDefinition = "text")
    private String text;

    private boolean liked;

    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean spoiler;

    // já tinha assistido antes (a mesma série, temporada ou episódio); o default preenche os registros antigos
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean rewatch;

    private LocalDate watchedOn;

    // registro em conjunto, em quem aceitou um convite: o convite que liga este registro ao de quem
    // convidou (nulo no de quem convidou, que tem os convites em `invites`)
    @ManyToOne
    @JoinColumn(name = "collaboration_id")
    private SeriesReviewCollaboration collaboration;

    // registro em conjunto, em quem convidou: um convite para cada pessoa marcada no "assisti com"
    @OneToMany(mappedBy = "authorReview")
    @OrderBy("id")
    private List<SeriesReviewCollaboration> invites = new ArrayList<>();

    private Instant createdAt = Instant.now();

    private Instant updatedAt = Instant.now();

    public SeriesReview() {
    }

    public SeriesReview(User user, Series series) {
        this.user = user;
        this.series = series;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Series getSeries() { return series; }
    public void setSeries(Series series) { this.series = series; }

    public Integer getSeasonNumber() { return seasonNumber; }
    public void setSeasonNumber(Integer seasonNumber) { this.seasonNumber = seasonNumber; }

    public Integer getEpisodeNumber() { return episodeNumber; }
    public void setEpisodeNumber(Integer episodeNumber) { this.episodeNumber = episodeNumber; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }

    public boolean isSpoiler() { return spoiler; }
    public void setSpoiler(boolean spoiler) { this.spoiler = spoiler; }

    public boolean isRewatch() { return rewatch; }
    public void setRewatch(boolean rewatch) { this.rewatch = rewatch; }

    public LocalDate getWatchedOn() { return watchedOn; }
    public void setWatchedOn(LocalDate watchedOn) { this.watchedOn = watchedOn; }

    public SeriesReviewCollaboration getCollaboration() { return collaboration; }
    public void setCollaboration(SeriesReviewCollaboration collaboration) { this.collaboration = collaboration; }

    public List<SeriesReviewCollaboration> getInvites() { return invites; }

    /** o registro de quem convidou: o próprio, se não entrou por convite */
    public SeriesReview reviewDeOrigem() {
        return collaboration == null ? this : collaboration.getAuthorReview();
    }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SeriesReview)) return false;
        return Objects.equals(id, ((SeriesReview) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
