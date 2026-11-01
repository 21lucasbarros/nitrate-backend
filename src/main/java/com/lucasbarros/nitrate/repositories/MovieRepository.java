package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface MovieRepository extends JpaRepository<Movie, Long> {
    Optional<Movie> findByTmdbId(Long tmdbId);
    boolean existsByTmdbId(Long tmdbId);
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
}
