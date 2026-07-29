package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.Movie;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovieRepository extends JpaRepository<Movie, Long> {
}
