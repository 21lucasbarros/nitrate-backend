package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.MovieUserStateDTO;
import com.lucasbarros.nitrate.dto.ReviewDTO;
import com.lucasbarros.nitrate.dto.UserProfileDTO;
import com.lucasbarros.nitrate.dto.WatchlistItemDTO;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.entities.WatchlistItem;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.repositories.WatchlistRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private WatchlistRepository watchlistRepository;
    @Autowired private MovieRepository movieRepository;

    @Transactional(readOnly = true)
    public UserProfileDTO perfil(String username) {
        return perfil(buscarPorUsername(username));
    }

    @Transactional(readOnly = true)
    public UserProfileDTO perfil(User user) {
        return new UserProfileDTO(
                user,
                reviewRepository.countByUser(user),
                reviewRepository.countByUserAndTextIsNotNull(user),
                reviewRepository.countByUserAndLikedTrue(user),
                watchlistRepository.countByUser(user));
    }

    @Transactional(readOnly = true)
    public List<ReviewDTO> reviews(String username) {
        User user = buscarPorUsername(username);
        List<ReviewDTO> dtos = new ArrayList<>();
        for (Review review : reviewRepository.findByUserOrderByCreatedAtDesc(user)) {
            dtos.add(new ReviewDTO(review));
        }
        return dtos;
    }

    @Transactional(readOnly = true)
    public List<WatchlistItemDTO> watchlist(String username) {
        User user = buscarPorUsername(username);
        List<WatchlistItemDTO> dtos = new ArrayList<>();
        for (WatchlistItem item : watchlistRepository.findByUserOrderByAddedAtDesc(user)) {
            dtos.add(new WatchlistItemDTO(item));
        }
        return dtos;
    }

    @Transactional(readOnly = true)
    public MovieUserStateDTO estadoDoFilme(User user, Long movieId) {
        Movie filme = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Filme não encontrado. Id: " + movieId));
        ReviewDTO review = reviewRepository.findByUserAndMovie(user, filme)
                .map(ReviewDTO::new)
                .orElse(null);
        return new MovieUserStateDTO(review, watchlistRepository.existsByUserAndMovie(user, filme));
    }

    private User buscarPorUsername(String username) {
        return userRepository.findByUsername(username.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + username));
    }
}
