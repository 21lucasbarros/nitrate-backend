package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.ReviewDTO;
import com.lucasbarros.nitrate.dto.ReviewRequestDTO;
import com.lucasbarros.nitrate.security.CustomUserDetails;
import com.lucasbarros.nitrate.services.ReviewService;
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

    @GetMapping("/recent")
    public List<ReviewDTO> recentes() {
        return reviewService.recentes();
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@AuthenticationPrincipal CustomUserDetails principal,
                                        @PathVariable Long id) {
        reviewService.deletar(principal.getUser(), id);
        return ResponseEntity.noContent().build();
    }
}
