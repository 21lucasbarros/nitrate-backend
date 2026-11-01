package com.lucasbarros.nitrate.recommender;

import com.lucasbarros.nitrate.entities.*;
import com.lucasbarros.nitrate.graph.MovieGraph;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// testes do grafo sem banco: os filmes são montados à mão
class RecommendationEngineTest {

    private long proximoId = 1;

    private Movie filme(String titulo, Integer ano, Director diretor, Franchise franquia, Genre... generos) {
        Movie m = new Movie(titulo, ano, 7.0, diretor, franquia);
        m.setId(proximoId++);
        for (Genre g : generos) m.getGenres().add(g);
        return m;
    }

    @Test
    void recomendaFilmeDaMesmaFranquiaComMotivo() {
        Franchise aranha = new Franchise("Homem-Aranha");
        Movie novoDia = filme("Um Novo Dia", 2026, null, aranha);
        Movie semVolta = filme("Sem Volta Para Casa", 2021, null, aranha);
        Movie semRelacao = filme("Toy Story 5", 1995, null, null);

        RecommendationEngine engine = new RecommendationEngine(new MovieGraph(List.of(novoDia, semVolta, semRelacao)));
        UserPreferences prefs = new UserPreferences();
        prefs.addLikedMovie(novoDia);

        List<RecommendationResult> resultado = engine.recommend(prefs, 5);

        assertEquals(1, resultado.size());
        assertEquals(semVolta, resultado.get(0).getMovie());
        assertTrue(resultado.get(0).getReasons().get(0).contains("mesma franquia"));
    }

    @Test
    void naoRecomendaOQueJaFoiCurtido() {
        Director nolan = new Director("Christopher Nolan");
        Movie a = filme("Oppenheimer", 2023, nolan, null);
        Movie b = filme("A Odisseia", 2026, nolan, null);

        RecommendationEngine engine = new RecommendationEngine(new MovieGraph(List.of(a, b)));
        UserPreferences prefs = new UserPreferences();
        prefs.addLikedMovie(a);
        prefs.addLikedMovie(b);

        assertTrue(engine.recommend(prefs, 5).isEmpty());
    }

    @Test
    void diretorFavoritoAumentaAPontuacao() {
        Director nolan = new Director("Christopher Nolan");
        Movie a = filme("Oppenheimer", 2023, nolan, null);
        Movie b = filme("A Odisseia", 2026, nolan, null);
        MovieGraph grafo = new MovieGraph(List.of(a, b));

        UserPreferences semFavorito = new UserPreferences();
        semFavorito.addLikedMovie(a);
        UserPreferences comFavorito = new UserPreferences();
        comFavorito.addLikedMovie(a);
        comFavorito.addFavoriteDirector("Christopher Nolan");

        double base = new RecommendationEngine(grafo).recommend(semFavorito, 5).get(0).getScore();
        double reforcado = new RecommendationEngine(grafo).recommend(comFavorito, 5).get(0).getScore();

        assertTrue(reforcado > base, "diretor favorito deveria pesar mais: " + reforcado + " vs " + base);
    }

    @Test
    void elencoEmComumConecta() {
        Actor zoe = new Actor("Zendaya");
        Movie a = filme("Duna", 2021, null, null);
        Movie b = filme("Rivais", 2024, null, null);
        a.getCast().add(new MovieCast(a, zoe, RoleType.PROTAGONIST));
        b.getCast().add(new MovieCast(b, zoe, RoleType.PROTAGONIST));

        UserPreferences prefs = new UserPreferences();
        prefs.addLikedMovie(a);
        List<RecommendationResult> resultado = new RecommendationEngine(new MovieGraph(List.of(a, b))).recommend(prefs, 5);

        assertEquals(b, resultado.get(0).getMovie());
        assertTrue(resultado.get(0).getReasons().get(0).contains("Zendaya"));
    }

    @Test
    void filmeSemAnoNaoQuebraOGrafo() {
        Genre drama = new Genre("Drama");
        Movie semAno = filme("Sem Ano", null, null, null, drama);
        Movie outro = filme("Outro Drama", 2020, null, null, drama);

        UserPreferences prefs = new UserPreferences();
        prefs.addLikedMovie(semAno);
        List<RecommendationResult> resultado = assertDoesNotThrow(
                () -> new RecommendationEngine(new MovieGraph(List.of(semAno, outro))).recommend(prefs, 5));

        assertEquals(outro, resultado.get(0).getMovie());
    }
}
