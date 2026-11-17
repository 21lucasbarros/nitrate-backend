package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.MovieList;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MovieListRepository extends JpaRepository<MovieList, Long> {
    List<MovieList> findByUserOrderByUpdatedAtDesc(User user);
    List<MovieList> findByUserAndPublicListTrueOrderByUpdatedAtDesc(User user);
}
