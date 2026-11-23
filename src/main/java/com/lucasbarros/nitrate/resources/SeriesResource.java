package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.MovieBackdropsDTO;
import com.lucasbarros.nitrate.dto.MovieStatsDTO;
import com.lucasbarros.nitrate.dto.ReviewCommentDTO;
import com.lucasbarros.nitrate.dto.ReviewCommentRequestDTO;
import com.lucasbarros.nitrate.dto.ReviewEngagementDTO;
import com.lucasbarros.nitrate.services.SeriesReviewSocialService;
import com.lucasbarros.nitrate.dto.SeasonDTO;
import com.lucasbarros.nitrate.dto.SeriesCollaborationAcceptDTO;
import com.lucasbarros.nitrate.dto.SeriesCollaborationDTO;
import com.lucasbarros.nitrate.dto.SeriesDTO;
import com.lucasbarros.nitrate.dto.SeriesDetailsDTO;
import com.lucasbarros.nitrate.dto.SeriesReviewDTO;
import com.lucasbarros.nitrate.dto.SeriesReviewRequestDTO;
import com.lucasbarros.nitrate.dto.SeriesSearchResultDTO;
import com.lucasbarros.nitrate.dto.WatchProvidersDTO;
import com.lucasbarros.nitrate.security.CustomUserDetails;
import com.lucasbarros.nitrate.services.SeriesDiaryService;
import com.lucasbarros.nitrate.services.SeriesReviewCollaborationService;
import com.lucasbarros.nitrate.services.SeriesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.util.List;

/** Séries: separadas de /movies, com reviews e watchlist próprias. */
@RestController
@RequestMapping("/series")
public class SeriesResource {

    @Autowired private SeriesService seriesService;
    @Autowired private SeriesDiaryService seriesDiaryService;
    @Autowired private SeriesReviewSocialService seriesReviewSocialService;
    @Autowired private SeriesReviewCollaborationService collaborationService;

    // literais têm prioridade sobre /{id}
    @GetMapping("/search")
    public List<SeriesSearchResultDTO> pesquisar(@RequestParam(defaultValue = "") String q) {
        return seriesService.pesquisar(Normalizer.normalize(q, Normalizer.Form.NFC));
    }

    @GetMapping("/trending")
    public List<SeriesSearchResultDTO> emAlta() {
        return seriesService.emAlta();
    }

    @GetMapping("/top-rated")
    public List<SeriesSearchResultDTO> maisBemAvaliadas() {
        return seriesService.maisBemAvaliadas();
    }

    // abre uma série do TMDB: importa pro banco na primeira vez e devolve a série local
    @GetMapping("/tmdb/{tmdbId}")
    public SeriesDTO obterPorTmdbId(@PathVariable Long tmdbId) {
        return seriesService.buscarPorId(seriesService.obterOuImportarId(tmdbId));
    }

    @GetMapping("/slug/{slug}")
    public SeriesDTO buscarPorSlug(@PathVariable String slug) {
        return seriesService.buscarPorSlug(slug);
    }

    @GetMapping("/{id}")
    public SeriesDTO buscarPorId(@PathVariable Long id) {
        return seriesService.buscarPorId(id);
    }

    @GetMapping("/{id}/details")
    public SeriesDetailsDTO detalhes(@PathVariable Long id) {
        return seriesService.detalhes(id);
    }

    @GetMapping("/{id}/seasons/{season}")
    public SeasonDTO temporada(@PathVariable Long id, @PathVariable int season) {
        return seriesService.temporada(id, season);
    }

    // as cenas da série, para escolher o fundo da imagem dos Stories
    @GetMapping("/{id}/backdrops")
    public MovieBackdropsDTO cenas(@PathVariable Long id) {
        return seriesService.cenas(id);
    }

    @GetMapping("/{id}/watch-providers")
    public WatchProvidersDTO ondeAssistir(@PathVariable Long id) {
        return seriesService.ondeAssistir(id);
    }

    @GetMapping("/{id}/stats")
    public MovieStatsDTO estatisticas(@PathVariable Long id) {
        return seriesService.estatisticas(id);
    }

    @GetMapping("/{id}/reviews")
    public List<SeriesReviewDTO> reviews(@PathVariable Long id) {
        return seriesDiaryService.daSerie(id);
    }

    // ---- registros (reviews de séries) ----

    // literal: tem prioridade sobre /reviews/{reviewId}
    @GetMapping("/reviews/recent")
    public List<SeriesReviewDTO> recentes(@RequestParam(defaultValue = "12") int limit) {
        return seriesDiaryService.recentes(limit);
    }

    @GetMapping("/reviews/{reviewId}")
    public SeriesReviewDTO review(@PathVariable Long reviewId) {
        return seriesDiaryService.buscarPorId(reviewId);
    }

    @PostMapping("/reviews")
    public ResponseEntity<SeriesReviewDTO> criar(@AuthenticationPrincipal CustomUserDetails principal,
                                                 @RequestBody SeriesReviewRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seriesDiaryService.criar(principal.getUser(), request));
    }

    @PutMapping("/reviews/{reviewId}")
    public SeriesReviewDTO atualizar(@AuthenticationPrincipal CustomUserDetails principal,
                                     @PathVariable Long reviewId,
                                     @RequestBody SeriesReviewRequestDTO request) {
        return seriesDiaryService.atualizar(principal.getUser(), reviewId, request);
    }

    // registro em conjunto: o convite que chega pelo sino para quem foi marcado no "assisti com"
    @GetMapping("/reviews/invites/{id}")
    public SeriesCollaborationDTO convite(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        return collaborationService.buscar(principal.getUser(), id);
    }

    // sem `review` no corpo, quem aceita fica com o registro de quem convidou
    @PostMapping("/reviews/invites/{id}/accept")
    public ResponseEntity<SeriesReviewDTO> aceitarConvite(@AuthenticationPrincipal CustomUserDetails principal,
                                                          @PathVariable Long id,
                                                          @RequestBody(required = false) SeriesCollaborationAcceptDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seriesDiaryService.aceitarColaboracao(
                principal.getUser(), id, request == null ? null : request.review()));
    }

    @PostMapping("/reviews/invites/{id}/decline")
    public ResponseEntity<Void> recusarConvite(@AuthenticationPrincipal CustomUserDetails principal,
                                               @PathVariable Long id) {
        collaborationService.recusar(principal.getUser(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/reviews/{reviewId}")
    public ResponseEntity<Void> deletar(@AuthenticationPrincipal CustomUserDetails principal,
                                        @PathVariable Long reviewId) {
        seriesDiaryService.deletar(principal.getUser(), reviewId);
        return ResponseEntity.noContent().build();
    }

    // curtidas e comentários: leitura pública (GET /series/** é liberado); com token, diz se você curtiu
    @GetMapping("/reviews/{reviewId}/engagement")
    public ReviewEngagementDTO engajamento(@AuthenticationPrincipal CustomUserDetails principal,
                                           @PathVariable Long reviewId) {
        return seriesReviewSocialService.engajamento(reviewId, principal == null ? null : principal.getUser());
    }

    @PostMapping("/reviews/{reviewId}/like")
    public ReviewEngagementDTO curtir(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long reviewId) {
        return seriesReviewSocialService.curtir(principal.getUser(), reviewId);
    }

    @DeleteMapping("/reviews/{reviewId}/like")
    public ReviewEngagementDTO descurtir(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long reviewId) {
        return seriesReviewSocialService.descurtir(principal.getUser(), reviewId);
    }

    @GetMapping("/reviews/{reviewId}/comments")
    public List<ReviewCommentDTO> comentarios(@PathVariable Long reviewId) {
        return seriesReviewSocialService.comentarios(reviewId);
    }

    @PostMapping("/reviews/{reviewId}/comments")
    public ResponseEntity<ReviewCommentDTO> comentar(@AuthenticationPrincipal CustomUserDetails principal,
                                                     @PathVariable Long reviewId,
                                                     @RequestBody ReviewCommentRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seriesReviewSocialService.comentar(principal.getUser(), reviewId, request.text()));
    }

    @DeleteMapping("/reviews/{reviewId}/comments/{commentId}")
    public ResponseEntity<Void> excluirComentario(@AuthenticationPrincipal CustomUserDetails principal,
                                                  @PathVariable Long reviewId,
                                                  @PathVariable Long commentId) {
        seriesReviewSocialService.excluirComentario(principal.getUser(), reviewId, commentId);
        return ResponseEntity.noContent().build();
    }

    // ---- watchlist de séries ----

    @PostMapping("/{id}/watchlist")
    public ResponseEntity<Void> adicionarNaWatchlist(@AuthenticationPrincipal CustomUserDetails principal,
                                                     @PathVariable Long id) {
        seriesDiaryService.adicionarNaWatchlist(principal.getUser(), id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/watchlist")
    public ResponseEntity<Void> removerDaWatchlist(@AuthenticationPrincipal CustomUserDetails principal,
                                                   @PathVariable Long id) {
        seriesDiaryService.removerDaWatchlist(principal.getUser(), id);
        return ResponseEntity.noContent().build();
    }
}
