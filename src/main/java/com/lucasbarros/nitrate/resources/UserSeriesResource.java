package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.SeriesReviewDTO;
import com.lucasbarros.nitrate.dto.SeriesUserStateDTO;
import com.lucasbarros.nitrate.security.CustomUserDetails;
import com.lucasbarros.nitrate.services.SeriesDiaryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** As séries de cada pessoa, à parte dos filmes do perfil. */
@RestController
@RequestMapping("/users")
public class UserSeriesResource {

    @Autowired private SeriesDiaryService seriesDiaryService;

    @GetMapping("/me/series/{seriesId}")
    public SeriesUserStateDTO estadoDaSerie(@AuthenticationPrincipal CustomUserDetails principal,
                                            @PathVariable Long seriesId) {
        return seriesDiaryService.estado(principal.getUser(), seriesId);
    }

    // o que as pessoas que eu sigo registraram de séries por último (a home junta com o feed de filmes)
    @GetMapping("/me/series-feed")
    public List<SeriesReviewDTO> feed(@AuthenticationPrincipal CustomUserDetails principal,
                                      @RequestParam(defaultValue = "12") int limit) {
        return seriesDiaryService.feed(principal.getUser(), limit);
    }

    @GetMapping("/{username}/series")
    public List<SeriesReviewDTO> series(@PathVariable String username) {
        return seriesDiaryService.doUsuario(username);
    }

    @GetMapping("/{username}/series-watchlist")
    public List<SeriesUserStateDTO.WatchlistItem> watchlist(@PathVariable String username) {
        return seriesDiaryService.watchlistDoUsuario(username);
    }
}
