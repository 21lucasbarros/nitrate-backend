package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.ReviewCommentDTO;
import com.lucasbarros.nitrate.dto.ReviewCommentRequestDTO;
import com.lucasbarros.nitrate.dto.ReviewDTO;
import com.lucasbarros.nitrate.dto.ReviewEngagementDTO;
import com.lucasbarros.nitrate.dto.ReviewRequestDTO;
import com.lucasbarros.nitrate.security.CustomUserDetails;
import com.lucasbarros.nitrate.services.ReviewService;
import com.lucasbarros.nitrate.services.ReviewSocialService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/reviews")
public class ReviewResource {

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ReviewSocialService reviewSocialService;

    @GetMapping("/recent")
    public List<ReviewDTO> recentes(@RequestParam(defaultValue = "20") int limit) {
        return reviewService.recentes(limit);
    }

    // pública: é a página da review (link "ver review completa")
    @GetMapping("/{id}")
    public ReviewDTO buscar(@PathVariable Long id) {
        return reviewService.buscarPorId(id);
    }

    @GetMapping("/featured")
    public List<ReviewDTO> destaques() {
        return reviewService.destaques();
    }

    @PostMapping
    public ResponseEntity<ReviewDTO> criar(@AuthenticationPrincipal CustomUserDetails principal,
                                           @RequestBody ReviewRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.criar(principal.getUser(), request));
    }

    @PutMapping("/{id}")
    public ReviewDTO atualizar(@AuthenticationPrincipal CustomUserDetails principal,
                               @PathVariable Long id,
                               @RequestBody ReviewRequestDTO request) {
        return reviewService.atualizar(principal.getUser(), id, request);
    }

    // curtidas e comentários: leitura pública; com token, diz também se você curtiu
    @GetMapping("/{id}/engagement")
    public ReviewEngagementDTO engajamento(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        return reviewSocialService.engajamento(id, principal == null ? null : principal.getUser());
    }

    @PostMapping("/{id}/like")
    public ReviewEngagementDTO curtir(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        return reviewSocialService.curtir(principal.getUser(), id);
    }

    @DeleteMapping("/{id}/like")
    public ReviewEngagementDTO descurtir(@AuthenticationPrincipal CustomUserDetails principal, @PathVariable Long id) {
        return reviewSocialService.descurtir(principal.getUser(), id);
    }

    @GetMapping("/{id}/comments")
    public List<ReviewCommentDTO> comentarios(@PathVariable Long id) {
        return reviewSocialService.comentarios(id);
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<ReviewCommentDTO> comentar(@AuthenticationPrincipal CustomUserDetails principal,
                                                     @PathVariable Long id,
                                                     @RequestBody ReviewCommentRequestDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(reviewSocialService.comentar(principal.getUser(), id, request.text()));
    }

    @DeleteMapping("/{id}/comments/{commentId}")
    public ResponseEntity<Void> excluirComentario(@AuthenticationPrincipal CustomUserDetails principal,
                                                  @PathVariable Long id,
                                                  @PathVariable Long commentId) {
        reviewSocialService.excluirComentario(principal.getUser(), id, commentId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@AuthenticationPrincipal CustomUserDetails principal,
                                        @PathVariable Long id) {
        reviewService.deletar(principal.getUser(), id);
        return ResponseEntity.noContent().build();
    }
}
