package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.entities.Director;
import com.lucasbarros.nitrate.entities.Genre;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.graph.MovieGraph;
import com.lucasbarros.nitrate.recommender.RecommendationResult;
import com.lucasbarros.nitrate.services.RecommendationService.Ajustes;
import com.lucasbarros.nitrate.services.RecommendationService.Entrada;
import com.lucasbarros.nitrate.services.RecommendationService.Registro;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

// o cálculo das recomendações sem banco: sem questionário, o service não usa nenhuma dependência
class RecommendationServiceTest {

    private final RecommendationService service = new RecommendationService();
    private long proximoId = 1;

    private Movie filme(String titulo, int ano, Director diretor, Genre... generos) {
        Movie m = new Movie(titulo, ano, 7.0, diretor, null);
        m.setId(proximoId++);
        for (Genre g : generos) m.getGenres().add(g);
        return m;
    }

    private static Registro curtido(Movie filme) {
        return new Registro(filme.getId(), 4.5, true, LocalDate.now().minusDays(3), Instant.now());
    }

    private static Entrada entrada(List<Registro> registros, List<Long> dispensados) {
        return new Entrada(1L, registros, List.of(), null, List.of(), dispensados);
    }

    private static List<Movie> filmes(List<RecommendationResult> resultado) {
        return resultado.stream().map(RecommendationResult::getMovie).toList();
    }

    @Test
    void dispensadoSomeEPesaContraOsParecidos() {
        Genre drama = new Genre("Drama");
        Director a = new Director("Diretora A");
        Director b = new Director("Diretor B");
        Movie curtido = filme("Curtido", 2020, null, drama);
        Movie dispensado = filme("Dispensado", 2020, a, drama);
        Movie tambemDeA = filme("Também de A", 2021, a, drama);
        Movie deB = filme("De B", 2021, b, drama);

        List<Movie> resultado = filmes(service.calcular(entrada(List.of(curtido(curtido)), List.of(dispensado.getId())),
                Ajustes.PADRAO, new MovieGraph(List.of(curtido, dispensado, tambemDeA, deB))));

        assertFalse(resultado.contains(dispensado));
        assertEquals(List.of(deB, tambemDeA), resultado);
    }

    @Test
    void notaBaixaPesaContraOsParecidos() {
        Genre drama = new Genre("Drama");
        Director a = new Director("Diretora A");
        Director b = new Director("Diretor B");
        Movie curtido = filme("Curtido", 2020, null, drama);
        Movie odiado = filme("Odiado", 2020, a, drama);
        Movie deA = filme("Também de A", 2021, a, drama);
        Movie deB = filme("De B", 2021, b, drama);
        Registro notaUm = new Registro(odiado.getId(), 1.0, false, LocalDate.now().minusDays(1), Instant.now());

        List<Movie> resultado = filmes(service.calcular(entrada(List.of(curtido(curtido), notaUm), List.of()),
                Ajustes.PADRAO, new MovieGraph(List.of(curtido, odiado, deA, deB))));

        assertEquals(List.of(deB, deA), resultado);
    }

    @Test
    void filmeQueAindaNaoEstreouFicaDeFora() {
        Director chazelle = new Director("Damien Chazelle");
        Movie curtido = filme("La La Land: Cantando Estações", 2016, chazelle);
        Movie lancado = filme("Babilônia", 2022, chazelle);
        Movie futuro = filme("Próximo do Chazelle", LocalDate.now().getYear() + 1, chazelle);

        List<RecommendationResult> resultado = service.calcular(entrada(List.of(curtido(curtido)), List.of()),
                Ajustes.PADRAO, new MovieGraph(List.of(curtido, lancado, futuro)));

        assertEquals(List.of(lancado), filmes(resultado));
        // o motivo cita o título só até o ":"
        assertEquals("mesmo diretor (Damien Chazelle), como em La La Land", resultado.get(0).getReasons().get(0));
    }

    @Test
    void filmeDesteAnoComEstreiaMarcadaParaDepoisFicaDeFora() {
        // Duna: Parte Três, a semanas da estreia: o ano já é o corrente, mas a data ainda não chegou
        Director villeneuve = new Director("Denis Villeneuve");
        Movie curtido = filme("Duna", 2021, villeneuve);
        Movie lancado = filme("Duna: Parte Dois", 2024, villeneuve);
        Movie emBreve = filme("Duna: Parte Três", LocalDate.now().getYear(), villeneuve);
        emBreve.setReleaseDate(LocalDate.now().plusDays(21));

        List<RecommendationResult> resultado = service.calcular(entrada(List.of(curtido(curtido)), List.of()),
                Ajustes.PADRAO, new MovieGraph(List.of(curtido, lancado, emBreve)));

        assertEquals(List.of(lancado), filmes(resultado));
    }

    @Test
    void sementeRecenteEBemAvaliadaPesaMais() {
        LocalDate hoje = LocalDate.now();
        double recenteCinco = RecommendationService.pesoDaSemente(new Registro(1L, 5.0, true, hoje, Instant.now()), hoje);
        double antigaTresEMeia = RecommendationService.pesoDaSemente(new Registro(2L, 3.5, false, hoje.minusYears(2), Instant.now()), hoje);
        assertTrue(recenteCinco > 2 * antigaTresEMeia, recenteCinco + " vs " + antigaTresEMeia);
    }

    @Test
    void sementeMaisRecenteLevaAPrimeiraVaga() {
        Director a = new Director("Diretora A");
        Director b = new Director("Diretor B");
        Movie antigo = filme("Visto há dois anos", 2018, a);
        Movie recente = filme("Visto ontem", 2024, b);
        Movie deA = filme("De A", 2019, a);
        Movie deB = filme("De B", 2019, b);
        List<Registro> registros = List.of(
                new Registro(antigo.getId(), 4.0, false, LocalDate.now().minusYears(2), Instant.now()),
                new Registro(recente.getId(), 4.0, false, LocalDate.now().minusDays(1), Instant.now()));

        List<Movie> resultado = filmes(service.calcular(entrada(registros, List.of()), Ajustes.PADRAO,
                new MovieGraph(List.of(antigo, recente, deA, deB))));

        assertEquals(List.of(deB, deA), resultado);
    }
}
