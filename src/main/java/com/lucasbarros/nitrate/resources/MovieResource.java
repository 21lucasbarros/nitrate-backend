package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.MovieDTO;
import com.lucasbarros.nitrate.services.MovieService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping(value = "/movies")
public class MovieResource {

    @Autowired
    private MovieService movieService;

    @GetMapping
    public List<MovieDTO> buscarTodos() {
        return movieService.buscarTodos();
    }

    @GetMapping(value = "/{id}")
    public MovieDTO buscarPorId(@PathVariable Long id) {
        return movieService.buscarPorId(id);
    }
}
