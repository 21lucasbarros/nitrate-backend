package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.dto.MovieBackdropsDTO;
import com.lucasbarros.nitrate.dto.MovieCreditsDTO;
import com.lucasbarros.nitrate.dto.MovieDTO;
import com.lucasbarros.nitrate.dto.MovieSearchResultDTO;
import com.lucasbarros.nitrate.dto.MovieStatsDTO;
import com.lucasbarros.nitrate.dto.PageDTO;
import com.lucasbarros.nitrate.dto.WatchProvidersDTO;
import com.lucasbarros.nitrate.dto.ReviewDTO;
import com.lucasbarros.nitrate.services.CatalogoFiltro;
import com.lucasbarros.nitrate.services.MovieService;
import com.lucasbarros.nitrate.services.ProfileCoverService;
import com.lucasbarros.nitrate.services.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.Normalizer;
import java.util.List;

@RestController
@RequestMapping(value = "/movies")
public class MovieResource {

    @Autowired
    private MovieService movieService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private ProfileCoverService profileCoverService;

    @GetMapping(value = "/catalog")
    public PageDTO<MovieDTO> catalogo(@RequestParam(defaultValue = "0") int page,
                                      @RequestParam(defaultValue = "48") int size,
                                      @RequestParam(required = false) List<String> genre,
                                      @RequestParam(required = false) Integer decade,
                                      @RequestParam(required = false) Double minTmdb,
                                      @RequestParam(required = false) Double minNitrate,
                                      @RequestParam(required = false) String sort) {
        return movieService.catalogo(page, size, CatalogoFiltro.de(genre, decade, minTmdb, minNitrate, sort));
    }

    @GetMapping(value = "/genres")
    public List<String> generos() {
        return movieService.generos();
    }

    // descoberta pela comunidade (literais: têm prioridade sobre /{id})
    @GetMapping(value = "/community/trending")
    public List<MovieDTO> emAlta() {
        return movieService.emAltaNaComunidade();
    }

    @GetMapping(value = "/community/top-rated")
    public List<MovieDTO> maisBemAvaliados() {
        return movieService.maisBemAvaliadosPelaComunidade();
    }

    @GetMapping(value = "/{id}")
    public MovieDTO buscarPorId(@PathVariable Long id) {
        return movieService.buscarPorId(id);
    }

    @GetMapping(value = "/{id}/reviews")
    public List<ReviewDTO> reviews(@PathVariable Long id) {
        return reviewService.doFilme(id);
    }

    @GetMapping(value = "/search")
    public List<MovieSearchResultDTO> pesquisar(@RequestParam(defaultValue = "") String q) {
        return movieService.pesquisar(Normalizer.normalize(q, Normalizer.Form.NFC));
    }

    // abre um filme do TMDB: importa pro banco na primeira vez e devolve o filme local
    @GetMapping(value = "/tmdb/{tmdbId}")
    public MovieDTO obterPorTmdbId(@PathVariable Long tmdbId) {
        Long id = movieService.obterOuImportarId(tmdbId);
        return movieService.buscarPorId(id);
    }

    /** cenas de qualquer filme do TMDB (importado ou não), para escolher o fundo do perfil */
    @GetMapping(value = "/tmdb/{tmdbId}/backdrops")
    public MovieBackdropsDTO cenas(@PathVariable Long tmdbId) {
        return profileCoverService.cenas(tmdbId);
    }

    @GetMapping(value = "/slug/{slug}")
    public MovieDTO buscarPorSlug(@PathVariable String slug) {
        return movieService.buscarPorSlug(slug);
    }

    @GetMapping(value = "/{id}/credits")
    public MovieCreditsDTO creditos(@PathVariable Long id) {
        return movieService.creditos(id);
    }

    @GetMapping(value = "/{id}/stats")
    public MovieStatsDTO estatisticas(@PathVariable Long id) {
        return movieService.estatisticas(id);
    }

    @GetMapping(value = "/{id}/watch-providers")
    public WatchProvidersDTO ondeAssistir(@PathVariable Long id) {
        return movieService.ondeAssistir(id);
    }
}
