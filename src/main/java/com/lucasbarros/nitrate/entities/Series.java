package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Uma série de TV, importada do TMDB na primeira vez que alguém a abre. Fica separada dos
 * filmes: tabela, reviews e watchlist próprias, fora do grafo e das recomendações de filmes.
 */
@Entity
@BatchSize(size = 50)
@Table(name = "tb_series")
public class Series implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long tmdbId;

    @Column(nullable = false)
    private String name;

    private Integer firstAirYear;

    // data do primeiro episódio; nula enquanto o TMDB não tem estreia marcada
    private LocalDate firstAirDate;

    // Radar de Estreias (ver SeriesRadarService): se a última verificação viu a série já no ar.
    // Nulo = nunca verificada
    private Boolean radarEstreou;

    // maior temporada (fora os especiais) já no ar na última verificação; nulo = nunca verificada
    private Integer radarUltimaTemporada;

    // quando o radar olhou a série pela última vez: as menos recentes vão primeiro
    private Instant radarVerificadoEm;

    // nulo enquanto a série ainda está no ar
    private Integer lastAirYear;

    // "Returning Series", "Ended", "Canceled"... como vem do TMDB
    private String status;

    private Integer numberOfSeasons;

    private Integer numberOfEpisodes;

    private Double rating;

    @Column(length = 1000)
    private String overview;

    private String posterPath;

    // quem criou a série, separados por vírgula ("Vince Gilligan")
    private String creators;

    @BatchSize(size = 50)
    @ElementCollection
    @CollectionTable(name = "tb_series_genre", joinColumns = @JoinColumn(name = "series_id"))
    @Column(name = "genre")
    private List<String> genres = new ArrayList<>();

    // "breaking-bad-2008" (mesmas regras do slug dos filmes)
    @Column(unique = true)
    private String slug;

    public Series() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getTmdbId() { return tmdbId; }
    public void setTmdbId(Long tmdbId) { this.tmdbId = tmdbId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Integer getFirstAirYear() { return firstAirYear; }
    public void setFirstAirYear(Integer firstAirYear) { this.firstAirYear = firstAirYear; }

    public LocalDate getFirstAirDate() { return firstAirDate; }
    public void setFirstAirDate(LocalDate firstAirDate) { this.firstAirDate = firstAirDate; }

    // como no filme: com a data, vale a data; sem ela, um ano futuro (ou série só anunciada) não estreou
    public boolean jaEstreou() {
        if (firstAirDate != null) return !firstAirDate.isAfter(LocalDate.now());
        if (firstAirYear != null) return firstAirYear <= LocalDate.now().getYear();
        return !"Planned".equals(status) && !"In Production".equals(status);
    }

    public Boolean getRadarEstreou() { return radarEstreou; }
    public void setRadarEstreou(Boolean radarEstreou) { this.radarEstreou = radarEstreou; }

    public Integer getRadarUltimaTemporada() { return radarUltimaTemporada; }
    public void setRadarUltimaTemporada(Integer radarUltimaTemporada) { this.radarUltimaTemporada = radarUltimaTemporada; }

    public Instant getRadarVerificadoEm() { return radarVerificadoEm; }
    public void setRadarVerificadoEm(Instant radarVerificadoEm) { this.radarVerificadoEm = radarVerificadoEm; }

    public Integer getLastAirYear() { return lastAirYear; }
    public void setLastAirYear(Integer lastAirYear) { this.lastAirYear = lastAirYear; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Integer getNumberOfSeasons() { return numberOfSeasons; }
    public void setNumberOfSeasons(Integer numberOfSeasons) { this.numberOfSeasons = numberOfSeasons; }

    public Integer getNumberOfEpisodes() { return numberOfEpisodes; }
    public void setNumberOfEpisodes(Integer numberOfEpisodes) { this.numberOfEpisodes = numberOfEpisodes; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public String getOverview() { return overview; }
    public void setOverview(String overview) { this.overview = overview; }

    public String getPosterPath() { return posterPath; }
    public void setPosterPath(String posterPath) { this.posterPath = posterPath; }

    public String getCreators() { return creators; }
    public void setCreators(String creators) { this.creators = creators; }

    public List<String> getGenres() { return genres; }
    public void setGenres(List<String> genres) { this.genres = genres; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Series)) return false;
        return Objects.equals(id, ((Series) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
