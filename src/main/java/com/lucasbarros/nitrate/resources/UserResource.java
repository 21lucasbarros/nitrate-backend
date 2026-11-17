package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.AccountDeletionDTO;
import com.lucasbarros.nitrate.dto.AccountExportDTO;
import com.lucasbarros.nitrate.dto.FavoritesRequestDTO;
import com.lucasbarros.nitrate.dto.FriendWatchlistItemDTO;
import com.lucasbarros.nitrate.dto.MovieDTO;
import com.lucasbarros.nitrate.dto.MovieListDTO;
import com.lucasbarros.nitrate.dto.MovieUserStateDTO;
import com.lucasbarros.nitrate.dto.NotificationDTO;
import com.lucasbarros.nitrate.dto.PageDTO;
import com.lucasbarros.nitrate.dto.ProfileUpdateDTO;
import com.lucasbarros.nitrate.dto.RecommendationResultDTO;
import com.lucasbarros.nitrate.dto.RetrospectiveDTO;
import com.lucasbarros.nitrate.dto.ReviewDTO;
import com.lucasbarros.nitrate.dto.SimilarUserDTO;
import com.lucasbarros.nitrate.dto.UserProfileDTO;
import com.lucasbarros.nitrate.dto.UserSummaryDTO;
import com.lucasbarros.nitrate.dto.WatchlistItemDTO;
import com.lucasbarros.nitrate.security.CustomUserDetails;
import com.lucasbarros.nitrate.services.AccountService;
import com.lucasbarros.nitrate.services.FriendsService;
import com.lucasbarros.nitrate.services.MovieListService;
import com.lucasbarros.nitrate.services.NotificationService;
import com.lucasbarros.nitrate.services.RecommendationService;
import com.lucasbarros.nitrate.services.RetrospectiveService;
import com.lucasbarros.nitrate.services.SocialService;
import com.lucasbarros.nitrate.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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

    @Autowired
    private AccountService accountService;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private MovieListService movieListService;

    @Autowired
    private FriendsService friendsService;

    @GetMapping("/me")
    public UserProfileDTO me(@AuthenticationPrincipal CustomUserDetails principal) {
        return userService.perfil(principal.getUser());
    }

    @PutMapping("/me/profile")
    public UserProfileDTO atualizarPerfil(@AuthenticationPrincipal CustomUserDetails principal,
                                          @RequestBody ProfileUpdateDTO request) {
        return accountService.atualizarPerfil(principal.getUser(), request);
    }

    // LGPD: cópia dos dados (acesso/portabilidade) e exclusão definitiva da conta
    @GetMapping("/me/export")
    public AccountExportDTO exportar(@AuthenticationPrincipal CustomUserDetails principal) {
        return accountService.exportar(principal.getUser());
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> excluirConta(@AuthenticationPrincipal CustomUserDetails principal,
                                             @RequestBody AccountDeletionDTO request) {
        accountService.excluirConta(principal.getUser(), request.password());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/me/notifications")
    public List<NotificationDTO> notificacoes(@AuthenticationPrincipal CustomUserDetails principal) {
        return notificationService.listar(principal.getUser());
    }

    @GetMapping("/me/notifications/unread-count")
    public Map<String, Long> notificacoesNaoLidas(@AuthenticationPrincipal CustomUserDetails principal) {
        return Map.of("count", notificationService.naoLidas(principal.getUser()));
    }

    @PostMapping("/me/notifications/read")
    public ResponseEntity<Void> marcarNotificacoesComoLidas(@AuthenticationPrincipal CustomUserDetails principal) {
        notificationService.marcarTodasComoLidas(principal.getUser());
        return ResponseEntity.noContent().build();
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

    // com movieId, cada lista diz se já tem o filme (o "adicionar à lista" da página do filme)
    @GetMapping("/me/lists")
    public List<MovieListDTO> minhasListas(@AuthenticationPrincipal CustomUserDetails principal,
                                           @RequestParam(required = false) Long movieId) {
        return movieListService.minhasListas(principal.getUser(), movieId);
    }

    // página "Amigos": o que quem eu sigo assistiu (paginado), listou e guardou para ver
    @GetMapping("/me/friends/activity")
    public PageDTO<ReviewDTO> atividadeDosAmigos(@AuthenticationPrincipal CustomUserDetails principal,
                                                 @RequestParam(defaultValue = "0") int page,
                                                 @RequestParam(defaultValue = "24") int size) {
        return friendsService.assistiram(principal.getUser(), page, size);
    }

    @GetMapping("/me/friends/lists")
    public List<MovieListDTO> listasDosAmigos(@AuthenticationPrincipal CustomUserDetails principal) {
        return friendsService.listas(principal.getUser());
    }

    @GetMapping("/me/friends/watchlist")
    public List<FriendWatchlistItemDTO> watchlistDosAmigos(@AuthenticationPrincipal CustomUserDetails principal) {
        return friendsService.watchlists(principal.getUser());
    }

    // quem avaliou os mesmos filmes com notas parecidas
    @GetMapping("/me/similar")
    public List<SimilarUserDTO> gostosParecidos(@AuthenticationPrincipal CustomUserDetails principal) {
        return socialService.gostosParecidos(principal.getUser());
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

    // rota pública; no próprio perfil (com token) vêm também as listas privadas
    @GetMapping("/{username}/lists")
    public List<MovieListDTO> listas(@AuthenticationPrincipal CustomUserDetails principal,
                                     @PathVariable String username) {
        return movieListService.listasDoUsuario(username, principal == null ? null : principal.getUser());
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
