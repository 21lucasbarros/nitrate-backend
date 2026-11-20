package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.AccountDeletionDTO;
import com.lucasbarros.nitrate.dto.AccountExportDTO;
import com.lucasbarros.nitrate.dto.AuthResponseDTO;
import com.lucasbarros.nitrate.dto.FavoritePeopleDTO;
import com.lucasbarros.nitrate.dto.FavoritePeopleRequestDTO;
import com.lucasbarros.nitrate.dto.FilmDTO;
import com.lucasbarros.nitrate.dto.FavoritePersonDTO;
import com.lucasbarros.nitrate.dto.FavoritesRequestDTO;
import com.lucasbarros.nitrate.entities.FavoritePerson;
import com.lucasbarros.nitrate.dto.ActivityPageDTO;
import com.lucasbarros.nitrate.dto.MovieDTO;
import com.lucasbarros.nitrate.dto.MovieListDTO;
import com.lucasbarros.nitrate.dto.MovieUserStateDTO;
import com.lucasbarros.nitrate.dto.NotificationDTO;
import com.lucasbarros.nitrate.dto.PageDTO;
import com.lucasbarros.nitrate.dto.PasswordChangeDTO;
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
import com.lucasbarros.nitrate.services.AvatarService;
import com.lucasbarros.nitrate.services.FavoritePeopleService;
import com.lucasbarros.nitrate.services.FriendsService;
import com.lucasbarros.nitrate.services.ListSocialService;
import com.lucasbarros.nitrate.services.MovieListService;
import com.lucasbarros.nitrate.services.NotificationService;
import com.lucasbarros.nitrate.services.RecommendationService;
import com.lucasbarros.nitrate.services.RetrospectiveService;
import com.lucasbarros.nitrate.services.SocialService;
import com.lucasbarros.nitrate.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.text.Normalizer;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
    private ListSocialService listSocialService;

    @Autowired
    private FriendsService friendsService;

    @Autowired
    private FavoritePeopleService favoritePeopleService;

    @Autowired
    private AvatarService avatarService;

    @GetMapping("/me")
    public UserProfileDTO me(@AuthenticationPrincipal CustomUserDetails principal) {
        return userService.perfil(principal.getUser());
    }

    @PutMapping("/me/profile")
    public UserProfileDTO atualizarPerfil(@AuthenticationPrincipal CustomUserDetails principal,
                                          @RequestBody ProfileUpdateDTO request) {
        return accountService.atualizarPerfil(principal.getUser(), request);
    }

    /** devolve um token novo: os anteriores, inclusive o desta requisição, deixam de valer */
    @PutMapping("/me/password")
    public AuthResponseDTO trocarSenha(@AuthenticationPrincipal CustomUserDetails principal,
                                       @RequestBody PasswordChangeDTO request) {
        return accountService.trocarSenha(principal.getUser(), request);
    }

    // foto de perfil: multipart com o campo "file" (JPG ou PNG); o servidor recorta e reduz
    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileDTO trocarFoto(@AuthenticationPrincipal CustomUserDetails principal,
                                     @RequestParam("file") MultipartFile file) {
        return avatarService.salvar(principal.getUser(), file);
    }

    @DeleteMapping("/me/avatar")
    public UserProfileDTO removerFoto(@AuthenticationPrincipal CustomUserDetails principal) {
        return avatarService.remover(principal.getUser());
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

    @PutMapping("/me/favorites/actors")
    public List<FavoritePersonDTO> definirAtoresFavoritos(@AuthenticationPrincipal CustomUserDetails principal,
                                                          @RequestBody FavoritePeopleRequestDTO request) {
        return favoritePeopleService.definirFavoritos(principal.getUser(), FavoritePerson.Kind.ACTOR, request.tmdbIds());
    }

    @PutMapping("/me/favorites/directors")
    public List<FavoritePersonDTO> definirDiretoresFavoritos(@AuthenticationPrincipal CustomUserDetails principal,
                                                             @RequestBody FavoritePeopleRequestDTO request) {
        return favoritePeopleService.definirFavoritos(principal.getUser(), FavoritePerson.Kind.DIRECTOR, request.tmdbIds());
    }

    // com movieId, cada lista diz se já tem o filme (o "adicionar à lista" da página do filme)
    @GetMapping("/me/lists")
    public List<MovieListDTO> minhasListas(@AuthenticationPrincipal CustomUserDetails principal,
                                           @RequestParam(required = false) Long movieId) {
        return movieListService.minhasListas(principal.getUser(), movieId);
    }

    // listas de outras pessoas que salvei
    @GetMapping("/me/saved-lists")
    public List<MovieListDTO> listasSalvas(@AuthenticationPrincipal CustomUserDetails principal) {
        return listSocialService.salvas(principal.getUser());
    }

    // página "Amigos": o feed de atividade meu e de quem eu sigo; `before` pede a próxima página
    @GetMapping("/me/friends/feed")
    public ActivityPageDTO feedDosAmigos(@AuthenticationPrincipal CustomUserDetails principal,
                                         @RequestParam(required = false) Instant before,
                                         @RequestParam(defaultValue = "30") int size) {
        return friendsService.feed(principal.getUser(), before, size);
    }

    // quem avaliou os mesmos filmes com notas parecidas
    @GetMapping("/me/similar")
    public List<SimilarUserDTO> gostosParecidos(@AuthenticationPrincipal CustomUserDetails principal) {
        return socialService.gostosParecidos(principal.getUser());
    }

    // literal: tem prioridade sobre /{username} ("search" é um nome reservado no cadastro)
    @GetMapping("/search")
    public List<UserSummaryDTO> pesquisar(@RequestParam(defaultValue = "") String q) {
        return socialService.pesquisarUsuarios(Normalizer.normalize(q, Normalizer.Form.NFC));
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

    // a URL leva a versão (?v=), então a resposta pode ficar em cache para sempre
    @GetMapping("/{username}/avatar")
    public ResponseEntity<byte[]> foto(@PathVariable String username) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .header("X-Content-Type-Options", "nosniff")
                .body(avatarService.imagem(username));
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

    // todos os filmes assistidos, sem repetir reassistidos; ordem: lancamento (padrão), lancamento-antigos,
    // nota, nota-menor, assistidos, titulo
    @GetMapping("/{username}/films")
    public PageDTO<FilmDTO> filmes(@PathVariable String username,
                                     @RequestParam(defaultValue = "0") int page,
                                     @RequestParam(defaultValue = "48") int size,
                                     @RequestParam(defaultValue = "lancamento") String sort,
                                     @RequestParam(defaultValue = "false") boolean liked) {
        return userService.filmes(username, page, size, sort, liked);
    }

        @GetMapping("/{username}/favorites/people")
    public FavoritePeopleDTO pessoasFavoritas(@PathVariable String username) {
        return favoritePeopleService.favoritos(username);
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

    // rota pública: a página da lista (/u/{username}/lista/{slug} no front); privadas só para a dona
    @GetMapping("/{username}/lists/{slug}")
    public MovieListDTO lista(@AuthenticationPrincipal CustomUserDetails principal,
                              @PathVariable String username,
                              @PathVariable String slug) {
        return movieListService.buscarPorSlug(username, slug, principal == null ? null : principal.getUser());
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
