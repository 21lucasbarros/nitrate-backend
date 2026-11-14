package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.FavoritesRequestDTO;
import com.lucasbarros.nitrate.dto.MovieDTO;
import com.lucasbarros.nitrate.dto.MovieUserStateDTO;
import com.lucasbarros.nitrate.dto.PageDTO;
import com.lucasbarros.nitrate.dto.RecommendationResultDTO;
import com.lucasbarros.nitrate.dto.RetrospectiveDTO;
import com.lucasbarros.nitrate.dto.ReviewDTO;
import com.lucasbarros.nitrate.dto.UserProfileDTO;
import com.lucasbarros.nitrate.dto.UserSummaryDTO;
import com.lucasbarros.nitrate.dto.WatchlistItemDTO;
import com.lucasbarros.nitrate.security.CustomUserDetails;
import com.lucasbarros.nitrate.services.RecommendationService;
import com.lucasbarros.nitrate.services.RetrospectiveService;
import com.lucasbarros.nitrate.services.SocialService;
import com.lucasbarros.nitrate.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
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

    @Autowired
    private RetrospectiveService retrospectiveService;

    @Autowired
    private SocialService socialService;

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

    // o que as pessoas que eu sigo registraram por último
    @GetMapping("/me/feed")
    public List<ReviewDTO> feed(@AuthenticationPrincipal CustomUserDetails principal,
                                @RequestParam(defaultValue = "12") int limit) {
        return socialService.feed(principal.getUser(), limit);
    }

    @PutMapping("/me/favorites")
    public List<MovieDTO> definirFavoritos(@AuthenticationPrincipal CustomUserDetails principal,
                                           @RequestBody FavoritesRequestDTO request) {
        return socialService.definirFavoritos(principal.getUser(), request.movieIds());
    }

    // literal: tem prioridade sobre /{username} ("search" é um nome reservado no cadastro)
    @GetMapping("/search")
    public List<UserSummaryDTO> pesquisar(@RequestParam(defaultValue = "") String q) {
        return socialService.pesquisarUsuarios(q);
    }

    // rota pública; com token, diz também se quem vê segue o perfil
    @GetMapping("/{username}")
    public UserProfileDTO perfil(@AuthenticationPrincipal CustomUserDetails principal,
                                 @PathVariable String username) {
        return userService.perfil(username, principal == null ? null : principal.getUser());
    }

    @PostMapping("/{username}/follow")
    public ResponseEntity<Void> seguir(@AuthenticationPrincipal CustomUserDetails principal,
                                       @PathVariable String username) {
        socialService.seguir(principal.getUser(), username);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{username}/follow")
    public ResponseEntity<Void> deixarDeSeguir(@AuthenticationPrincipal CustomUserDetails principal,
                                               @PathVariable String username) {
        socialService.deixarDeSeguir(principal.getUser(), username);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{username}/followers")
    public List<UserSummaryDTO> seguidores(@PathVariable String username) {
        return socialService.seguidores(username);
    }

    @GetMapping("/{username}/following")
    public List<UserSummaryDTO> seguindo(@PathVariable String username) {
        return socialService.seguindo(username);
    }

    @GetMapping("/{username}/reviews")
    public PageDTO<ReviewDTO> reviews(@PathVariable String username,
                                      @RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "30") int size,
                                      @RequestParam(defaultValue = "false") boolean withText) {
        return userService.reviews(username, page, size, withText);
    }

    @GetMapping("/{username}/watchlist")
    public List<WatchlistItemDTO> watchlist(@PathVariable String username) {
        return userService.watchlist(username);
    }

    // a retrospectiva é exclusiva de quem é dono do diário: só existe em /users/me
    @GetMapping("/me/retrospective")
    public RetrospectiveDTO retrospectiva(@AuthenticationPrincipal CustomUserDetails principal,
                                          @RequestParam String month) {
        return retrospectiveService.retrospectiva(principal.getUser(), month);
    }

    @GetMapping("/me/retrospective/months")
    public List<String> mesesComRetrospectiva(@AuthenticationPrincipal CustomUserDetails principal) {
        return retrospectiveService.mesesDisponiveis(principal.getUser());
    }
}
