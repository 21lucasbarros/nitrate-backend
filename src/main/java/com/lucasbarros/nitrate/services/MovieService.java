package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.MovieDTO;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class MovieService {
    @Autowired
    private MovieRepository movieRepository;

    @Transactional(readOnly = true)
    public List<MovieDTO> buscarTodos() {
        List<Movie> filmes = movieRepository.findAll();
        List<MovieDTO> dtos = new ArrayList<>();
        for(Movie filme : filmes) {
            dtos.add(new MovieDTO(filme));
        }
        return dtos;
    }

    @Transactional(readOnly = true)
    public MovieDTO buscarPorId(Long id) {
        Movie filme = movieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Filme não encontrado. Id: " + id));
        return new MovieDTO(filme);
    }
}
