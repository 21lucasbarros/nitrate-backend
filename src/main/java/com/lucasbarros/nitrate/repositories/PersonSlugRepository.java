package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.PersonSlug;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PersonSlugRepository extends JpaRepository<PersonSlug, Long> {
    Optional<PersonSlug> findBySlug(String slug);
    List<PersonSlug> findByTmdbIdIn(Collection<Long> tmdbIds);
    boolean existsBySlug(String slug);

    // slugs que começam com a base, para achar o próximo sufixo livre de uma vez
    @Query("select p.slug from PersonSlug p where p.slug = :base or p.slug like concat(:base, '-%')")
    List<String> findSlugsComBase(@Param("base") String base);
}
