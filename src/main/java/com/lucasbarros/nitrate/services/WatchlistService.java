package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.LetterboxdImportDTO;
import com.lucasbarros.nitrate.dto.WatchlistItemDTO;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.entities.WatchlistItem;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.repositories.WatchlistRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
public class WatchlistService {

    @Autowired private WatchlistRepository watchlistRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private ReviewRepository reviewRepository;

    // idempotente: adicionar um filme que já está na lista só devolve o item existente
    @Transactional
    public WatchlistItemDTO adicionar(User user, Long movieId) {
        Movie filme = buscarFilme(movieId);
        WatchlistItem item = watchlistRepository.findByUserAndMovie(user, filme)
                .orElseGet(() -> watchlistRepository.save(new WatchlistItem(user, filme)));
        return new WatchlistItemDTO(item);
    }

    /** importação: adiciona só se a pessoa ainda não assistiu (não tem review) nem já tem na lista */
    @Transactional
    public LetterboxdImportDTO.Status adicionarSeNaoAssistiu(User user, Long movieId, LocalDate adicionadoEm) {
        Movie filme = buscarFilme(movieId);
        if (reviewRepository.findByUserAndMovie(user, filme).isPresent()) {
            return LetterboxdImportDTO.Status.ALREADY_WATCHED;
        }
        if (watchlistRepository.existsByUserAndMovie(user, filme)) {
            return LetterboxdImportDTO.Status.ALREADY_IN_WATCHLIST;
        }
        WatchlistItem item = new WatchlistItem(user, filme);
        if (adicionadoEm != null) {
            item.setAddedAt(adicionadoEm.atStartOfDay(ZoneOffset.UTC).toInstant());
        }
        watchlistRepository.save(item);
        return LetterboxdImportDTO.Status.WATCHLISTED;
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
