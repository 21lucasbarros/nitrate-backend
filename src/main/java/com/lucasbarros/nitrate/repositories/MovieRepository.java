package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findByTmdbId(Long tmdbId);
    boolean existsByTmdbId(Long tmdbId);
    Optional<Movie> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Movie> findBySlugIsNull();

    @Query("select m.slug from Movie m where m.slug is not null")
    List<String> findAllSlugs();
    List<Movie> findByTmdbIdIn(Collection<Long> tmdbIds);

    // o grafo carrega o acervo em duas consultas: buscar gêneros e elenco juntos multiplicaria
    // as linhas e duplicaria os itens da lista de elenco (cada ator contaria várias vezes)
    @Query("select distinct m from Movie m " +
            "left join fetch m.director left join fetch m.franchise left join fetch m.genres")
    List<Movie> findAllParaGrafo();

    @Query("select distinct m from Movie m left join fetch m.cast c left join fetch c.actor where m in :filmes")
    List<Movie> carregarElenco(@Param("filmes") Collection<Movie> filmes);

    @Modifying
    @Transactional
    @Query("update Movie m set m.grafoExpandido = true where m.id = :id")
    void marcarGrafoExpandido(@Param("id") Long id);

    // Radar de Estreias: grava só o que a verificação encontrou, sem regravar o filme inteiro
    @Modifying
    @Transactional
    @Query("update Movie m set m.releaseDate = :estreia, m.radarNosCinemas = :nosCinemas, " +
            "m.radarNoStreaming = :noStreaming where m.id = :id")
    void atualizarRadar(@Param("id") Long id, @Param("estreia") LocalDate estreia,
                        @Param("nosCinemas") boolean nosCinemas, @Param("noStreaming") boolean noStreaming);
}
