package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.FriendWatchlistItemDTO;
import com.lucasbarros.nitrate.dto.MovieListDTO;
import com.lucasbarros.nitrate.dto.PageDTO;
import com.lucasbarros.nitrate.dto.ReviewDTO;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.MovieListRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.repositories.WatchlistRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** A página "Amigos": o que as pessoas que você segue assistiram, listaram e guardaram para ver. */
@Service
public class FriendsService {

    private static final int PAGINA_MAX = 48;
    private static final int LISTAS_MAX = 30;
    private static final int WATCHLIST_MAX = 60;

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private MovieListRepository movieListRepository;
    @Autowired private WatchlistRepository watchlistRepository;

    @Transactional(readOnly = true)
    public PageDTO<ReviewDTO> assistiram(User eu, int pagina, int tamanho) {
        PageRequest pageRequest = PageRequest.of(Math.max(0, pagina), Math.min(Math.max(1, tamanho), PAGINA_MAX));
        return PageDTO.of(reviewRepository.findFeedPaginado(eu, pageRequest), ReviewDTO::new);
    }

    @Transactional(readOnly = true)
    public List<MovieListDTO> listas(User eu) {
        return movieListRepository.findDosAmigos(eu, PageRequest.of(0, LISTAS_MAX)).stream()
                .map(l -> MovieListDTO.resumo(l, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FriendWatchlistItemDTO> watchlists(User eu) {
        return watchlistRepository.findDosAmigos(eu, PageRequest.of(0, WATCHLIST_MAX)).stream()
                .map(FriendWatchlistItemDTO::new)
                .toList();
    }
}
