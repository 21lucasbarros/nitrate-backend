package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.entities.Movie;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MovieSlugServiceTest {

    private String slug(String titulo, Integer ano, Long tmdbId) {
        Movie m = new Movie(titulo, ano, null, null, null);
        m.setTmdbId(tmdbId);
        return MovieSlugService.base(m);
    }

    @Test
    void tituloEAno() {
        assertEquals("superman-2025", slug("Superman", 2025, 1L));
    }

    @Test
    void removeAcentosEPontuacao() {
        assertEquals("homem-aranha-sem-volta-para-casa-2021", slug("Homem-Aranha: Sem Volta Para Casa", 2021, 1L));
        assertEquals("500-dias-com-ela-2009", slug("(500) Dias com Ela", 2009, 1L));
        assertEquals("tick-tick-boom-2021", slug("tick, tick...BOOM!", 2021, 1L));
    }

    @Test
    void semAnoOuSemLetrasLatinas() {
        assertEquals("zodiaco", slug("Zodíaco", null, 1L));
        assertEquals("filme-123-2024", slug("愛のぬくもり", 2024, 123L));
    }
}
