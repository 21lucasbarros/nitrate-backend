package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.WatchlistItemDTO;
import com.lucasbarros.nitrate.security.CustomUserDetails;
import com.lucasbarros.nitrate.services.WatchlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/watchlist")
public class WatchlistResource {

    @Autowired
    private WatchlistService watchlistService;

    @PostMapping("/{movieId}")
    public WatchlistItemDTO adicionar(@AuthenticationPrincipal CustomUserDetails principal,
                                      @PathVariable Long movieId) {
        return watchlistService.adicionar(principal.getUser(), movieId);
    }

    @DeleteMapping("/{movieId}")
    public ResponseEntity<Void> remover(@AuthenticationPrincipal CustomUserDetails principal,
                                        @PathVariable Long movieId) {
        watchlistService.remover(principal.getUser(), movieId);
        return ResponseEntity.noContent().build();
    }
}
