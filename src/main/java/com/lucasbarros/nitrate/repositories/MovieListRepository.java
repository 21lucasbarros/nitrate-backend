package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.MovieList;
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

    @Query("select l.slug from MovieList l where l.user = :user and l.slug is not null and l.id <> :exceto")
    List<String> slugsDoUsuario(@Param("user") User user, @Param("exceto") Long exceto);

    // feed de atividade: listas públicas criadas por mim ou por quem eu sigo
    @Query("select l from MovieList l where l.publicList = true and (l.user = :me or l.user in " +
            "(select f.followed from Follow f where f.follower = :me)) " +
            "and l.createdAt < :antes order by l.createdAt desc, l.id desc")
    List<MovieList> findAtividade(@Param("me") User me, @Param("antes") Instant antes, Pageable pageable);
}
