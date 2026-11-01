package com.lucasbarros.nitrate.resources;

import com.lucasbarros.nitrate.graph.MovieGraphCache;
import com.lucasbarros.nitrate.tmdb.TmdbImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/import-movies")
public class ImportResource {

    @Autowired
    private TmdbImportService tmdbImportService;

    @Autowired
    private MovieGraphCache graphCache;

    // TODO: restringir esse endpoint (exigir um papel ADMIN) antes de ir pra produção.
    @PostMapping
    public String importar(@RequestParam(defaultValue = "2") int paginas) {
        int total = tmdbImportService.importarFilmesPopulares(paginas);
        graphCache.invalidar();
        return total + " filme(s) importado(s) da TMDB.";
    }
}