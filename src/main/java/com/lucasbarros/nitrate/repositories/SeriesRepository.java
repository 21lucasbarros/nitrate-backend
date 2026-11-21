package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Series;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SeriesRepository extends JpaRepository<Series, Long> {
    Optional<Series> findByTmdbId(Long tmdbId);
    Optional<Series> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Series> findByTmdbIdIn(Collection<Long> tmdbIds);

    // Radar de Estreias: séries que alguém tem na watchlist ou já registrou e que ainda podem ter
    // novidade: não estrearam (ou nunca foram verificadas e podem ter estreado há pouco) ou
    // seguem no ar e podem ganhar temporada nova. As verificadas há mais tempo vêm primeiro
    @Query("select s.id from Series s " +
            "where (exists (select 1 from SeriesWatchlistItem w where w.series = s) " +
            "    or exists (select 1 from SeriesReview r where r.series = s)) " +
            "and (((s.radarEstreou is null or s.radarEstreou = false) " +
            "      and (s.firstAirDate is null or s.firstAirDate >= :desde)) " +
            "  or (s.radarEstreou = true and s.lastAirYear is null)) " +
            "order by s.radarVerificadoEm asc nulls first, s.id")
    List<Long> findIdsParaRadar(@Param("desde") LocalDate desde);

    // Radar de Estreias: grava só o que a verificação encontrou, sem regravar a série inteira
    @Modifying
    @Transactional
    @Query("update Series s set s.firstAirDate = :estreia, s.radarEstreou = :estreou, " +
            "s.radarUltimaTemporada = :ultimaTemporada, s.status = :status, s.lastAirYear = :lastAirYear, " +
            "s.radarVerificadoEm = :agora where s.id = :id")
    void atualizarRadar(@Param("id") Long id, @Param("estreia") LocalDate estreia, @Param("estreou") boolean estreou,
                        @Param("ultimaTemporada") Integer ultimaTemporada, @Param("status") String status,
                        @Param("lastAirYear") Integer lastAirYear, @Param("agora") Instant agora);
}
