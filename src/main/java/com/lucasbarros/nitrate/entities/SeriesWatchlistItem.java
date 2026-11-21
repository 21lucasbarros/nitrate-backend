package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "tb_series_watchlist",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "series_id"}))
public class SeriesWatchlistItem implements Serializable {
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

    private Instant addedAt = Instant.now();

    public SeriesWatchlistItem() {
    }

    public SeriesWatchlistItem(User user, Series series) {
        this.user = user;
        this.series = series;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Series getSeries() { return series; }
    public void setSeries(Series series) { this.series = series; }

    public Instant getAddedAt() { return addedAt; }
    public void setAddedAt(Instant addedAt) { this.addedAt = addedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SeriesWatchlistItem)) return false;
        return Objects.equals(id, ((SeriesWatchlistItem) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
