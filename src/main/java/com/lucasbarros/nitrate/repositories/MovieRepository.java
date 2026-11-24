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

    // filtro do catálogo: só os gêneros que algum filme tem
    @Query("select distinct g.name from Movie m join m.genres g")
    List<String> findNomesDeGeneros();

    @Query("select m.slug from Movie m where m.slug is not null")
    List<String> findAllSlugs();
    List<Movie> findByTmdbIdIn(Collection<Long> tmdbIds);

    // o grafo carrega o acervo em três consultas só com as colunas que usa: buscar as entidades
    // com join fetch repetia a linha inteira do filme (com o overview) a cada gênero e a cada
    // membro do elenco, dezenas de MB por montagem
    @Query("select m.id, m.title, m.releaseYear, m.rating, m.tmdbId, m.slug, m.posterPath, m.releaseDate, " +
            "d.id, d.name, f.id, f.name from Movie m left join m.director d left join m.franchise f")
    List<Object[]> findResumosParaGrafo();

    @Query("select m.id, g.id, g.name from Movie m join m.genres g")
    List<Object[]> findGenerosParaGrafo();

    @Query("select c.movie.id, a.id, a.name, c.role from MovieCast c join c.actor a")
    List<Object[]> findElencoParaGrafo();

    @Modifying
    @Transactional
    @Query("update Movie m set m.grafoExpandido = true where m.id = :id")
    void marcarGrafoExpandido(@Param("id") Long id);

    // filmes importados antes de a data de estreia existir. Linhas: [Long id, Long tmdbId]
    @Query("select m.id, m.tmdbId from Movie m where m.releaseDate is null and m.tmdbId is not null order by m.id")
    List<Object[]> findSemDataDeEstreia();

    // os mesmos, mas só os que estão no diário de alguém (é neles que a ordem por lançamento aparece)
    @Query("select distinct r.movie.id, r.movie.tmdbId from Review r " +
            "where r.movie.releaseDate is null and r.movie.tmdbId is not null")
    List<Object[]> findSemDataDeEstreiaEmDiarios();

    @Modifying
    @Transactional
    @Query("update Movie m set m.releaseDate = :estreia where m.id = :id")
    void atualizarDataDeEstreia(@Param("id") Long id, @Param("estreia") LocalDate estreia);

    // Radar de Estreias: grava só o que a verificação encontrou, sem regravar o filme inteiro
    @Modifying
    @Transactional
    @Query("update Movie m set m.releaseDate = :estreia, m.radarNosCinemas = :nosCinemas, " +
            "m.radarNoStreaming = :noStreaming where m.id = :id")
    void atualizarRadar(@Param("id") Long id, @Param("estreia") LocalDate estreia,
                        @Param("nosCinemas") boolean nosCinemas, @Param("noStreaming") boolean noStreaming);
}
