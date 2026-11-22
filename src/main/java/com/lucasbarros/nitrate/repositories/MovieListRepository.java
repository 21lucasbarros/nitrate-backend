package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.MovieList;
import com.lucasbarros.nitrate.entities.MovieListEntry;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface MovieListRepository extends JpaRepository<MovieList, Long> {
    List<MovieList> findByUserOrderByUpdatedAtDesc(User user);
    List<MovieList> findByUserAndPublicListTrueOrderByUpdatedAtDesc(User user);
    Optional<MovieList> findByUserAndSlug(User user, String slug);
    List<MovieList> findBySlugIsNull();
    // home: listas públicas (e não vazias) de quem eu sigo, das atualizadas por último
    @Query("select l from MovieList l where l.publicList = true and l.entries is not empty and l.user in " +
            "(select f.followed from Follow f where f.follower = :me) order by l.updatedAt desc, l.id desc")
    List<MovieList> findDosAmigos(@Param("me") User me, Pageable pageable);

    @Query("select l.slug from MovieList l where l.user = :user and l.slug is not null and l.id <> :exceto")
    List<String> slugsDoUsuario(@Param("user") User user, @Param("exceto") Long exceto);

    // feed de atividade: listas públicas criadas por mim ou por quem eu sigo
    @Query("select l from MovieList l where l.publicList = true and (l.user = :me or l.user in " +
            "(select f.followed from Follow f where f.follower = :me)) " +
            "and l.createdAt < :antes order by l.createdAt desc, l.id desc")
    List<MovieList> findAtividade(@Param("me") User me, @Param("antes") Instant antes, Pageable pageable);

    // atividade com um filme: as listas da pessoa (públicas ou não) em que ele está
    @Query("select e from MovieListEntry e where e.list.user = :user and e.movie = :movie")
    List<MovieListEntry> findEntradasDoFilme(@Param("user") User user, @Param("movie") Movie movie);
}
