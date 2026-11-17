package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.MovieList;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MovieListRepository extends JpaRepository<MovieList, Long> {
    List<MovieList> findByUserOrderByUpdatedAtDesc(User user);
    List<MovieList> findByUserAndPublicListTrueOrderByUpdatedAtDesc(User user);

    // página "Amigos": as listas públicas de quem `user` segue, as mexidas por último primeiro
    @Query("select l from MovieList l where l.publicList = true and l.user in " +
            "(select f.followed from Follow f where f.follower = :user) order by l.updatedAt desc, l.id desc")
    List<MovieList> findDosAmigos(@Param("user") User user, Pageable pageable);
}
