package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.WatchlistItemDTO;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.entities.WatchlistItem;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.repositories.WatchlistRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WatchlistService {

    @Autowired private WatchlistRepository watchlistRepository;
    @Autowired private MovieRepository movieRepository;

    // idempotente: adicionar um filme que já está na lista só devolve o item existente
    @Transactional
    public WatchlistItemDTO adicionar(User user, Long movieId) {
        Movie filme = buscarFilme(movieId);
        WatchlistItem item = watchlistRepository.findByUserAndMovie(user, filme)
                .orElseGet(() -> watchlistRepository.save(new WatchlistItem(user, filme)));
        return new WatchlistItemDTO(item);
    }

    @Transactional
    public void remover(User user, Long movieId) {
        Movie filme = buscarFilme(movieId);
        watchlistRepository.findByUserAndMovie(user, filme).ifPresent(watchlistRepository::delete);
    }

    private Movie buscarFilme(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Filme não encontrado. Id: " + movieId));
    }
}
