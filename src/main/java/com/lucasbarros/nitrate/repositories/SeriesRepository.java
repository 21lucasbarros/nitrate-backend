package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Series;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SeriesRepository extends JpaRepository<Series, Long> {
    Optional<Series> findByTmdbId(Long tmdbId);
    Optional<Series> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Series> findByTmdbIdIn(Collection<Long> tmdbIds);
}
