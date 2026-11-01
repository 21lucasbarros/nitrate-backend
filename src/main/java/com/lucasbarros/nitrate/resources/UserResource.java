package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.MovieUserStateDTO;
import com.lucasbarros.nitrate.dto.RecommendationResultDTO;
import com.lucasbarros.nitrate.dto.ReviewDTO;
import com.lucasbarros.nitrate.dto.UserProfileDTO;
import com.lucasbarros.nitrate.dto.WatchlistItemDTO;
import com.lucasbarros.nitrate.security.CustomUserDetails;
import com.lucasbarros.nitrate.services.RecommendationService;
import com.lucasbarros.nitrate.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserResource {

    @Autowired
    private UserService userService;

    @Autowired
    private RecommendationService recommendationService;

    @GetMapping("/me")
    public UserProfileDTO me(@AuthenticationPrincipal CustomUserDetails principal) {
        return userService.perfil(principal.getUser());
    }

    @GetMapping("/me/recommendations")
    public List<RecommendationResultDTO> recomendacoes(@AuthenticationPrincipal CustomUserDetails principal) {
        return recommendationService.recomendarParaUsuario(principal.getUser());
    }

    @GetMapping("/me/movies/{movieId}")
    public MovieUserStateDTO estadoDoFilme(@AuthenticationPrincipal CustomUserDetails principal,
                                           @PathVariable Long movieId) {
        return userService.estadoDoFilme(principal.getUser(), movieId);
    }

    @GetMapping("/{username}")
    public UserProfileDTO perfil(@PathVariable String username) {
        return userService.perfil(username);
    }

    @GetMapping("/{username}/reviews")
    public List<ReviewDTO> reviews(@PathVariable String username) {
        return userService.reviews(username);
    }

    @GetMapping("/{username}/watchlist")
    public List<WatchlistItemDTO> watchlist(@PathVariable String username) {
        return userService.watchlist(username);
    }
}
