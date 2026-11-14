package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Endereço legível de cada filme, no estilo do Letterboxd: título + ano ("superman-2025").
 * Títulos repetidos no mesmo ano ganham um sufixo ("-2"). Na inicialização, preenche o slug
 * dos filmes que ainda não têm.
 */
@Service
public class MovieSlugService implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MovieSlugService.class);
    private static final int TAMANHO_MAX = 80;

    @Autowired private MovieRepository movieRepository;

    /** define o slug de um filme ainda não salvo (ou sem slug) */
    public void atribuir(Movie filme) {
        filme.setSlug(livre(base(filme), movieRepository::existsBySlug));
    }

    /**
     * Preenche o slug dos filmes que ainda não têm. Tudo em memória e salvo de uma vez: uma
     * consulta por filme levaria minutos num banco remoto (e a instância pode ser desligada).
     */
    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<Movie> semSlug = movieRepository.findBySlugIsNull();
        if (semSlug.isEmpty()) return;

        Set<String> usados = new HashSet<>(movieRepository.findAllSlugs());
        for (Movie filme : semSlug) {
            String slug = livre(base(filme), usados::contains);
            usados.add(slug);
            filme.setSlug(slug);
        }
        movieRepository.saveAll(semSlug);
        log.info("slug gerado para {} filme(s)", semSlug.size());
    }

    // títulos repetidos no mesmo ano: superman-2025, superman-2025-2, ...
    private static String livre(String base, Predicate<String> emUso) {
        String slug = base;
        for (int i = 2; emUso.test(slug); i++) {
            slug = base + "-" + i;
        }
        return slug;
    }

    static String base(Movie filme) {
        String texto = Normalizer.normalize(filme.getTitle() == null ? "" : filme.getTitle(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")      // remove acentos
                .toLowerCase()
                .replaceAll("[^a-z0-9]+", "-") // o resto vira hífen
                .replaceAll("(^-+|-+$)", "");
        if (texto.length() > TAMANHO_MAX) texto = texto.substring(0, TAMANHO_MAX).replaceAll("-+$", "");
        // títulos sem letras latinas (ex.: japonês) usam o id do TMDB
        if (texto.isEmpty()) texto = "filme-" + filme.getTmdbId();
        return filme.getReleaseYear() == null ? texto : texto + "-" + filme.getReleaseYear();
    }
}
