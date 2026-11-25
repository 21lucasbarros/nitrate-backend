package com.lucasbarros.nitrate.recommender;

import com.lucasbarros.nitrate.entities.*;
import com.lucasbarros.nitrate.graph.MovieGraph;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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

    @Test
    void motivoCitaOsFilmesCurtidosQueOGeraram() {
        Director nolan = new Director("Christopher Nolan");
        Movie oppenheimer = filme("Oppenheimer", 2023, nolan, null);
        Movie tenet = filme("Tenet", 2020, nolan, null);
        Movie odisseia = filme("A Odisseia", 2026, nolan, null);

        UserPreferences prefs = new UserPreferences();
        prefs.addLikedMovie(oppenheimer);
        prefs.addLikedMovie(tenet);
        List<RecommendationResult> resultado = new RecommendationEngine(new MovieGraph(List.of(oppenheimer, tenet, odisseia))).recommend(prefs, 5);

        assertEquals("mesmo diretor (Christopher Nolan), como em Oppenheimer e Tenet", resultado.get(0).getReasons().get(0));
    }

    @Test
    void generoComumPesaMenosQueGeneroRaro() {
        Genre drama = new Genre("Drama");
        Genre faroeste = new Genre("Faroeste");
        Movie curtidoDrama = filme("Drama Curtido", 2020, null, null, drama);
        Movie outroDrama = filme("Outro Drama", 2021, null, null, drama);
        Movie curtidoFaroeste = filme("Faroeste Curtido", 2020, null, null, faroeste);
        Movie outroFaroeste = filme("Outro Faroeste", 2021, null, null, faroeste);
        // catálogo com 20 dramas e só 2 faroestes
        List<Movie> catalogo = new java.util.ArrayList<>(List.of(curtidoDrama, outroDrama, curtidoFaroeste, outroFaroeste));
        for (int i = 0; i < 18; i++) catalogo.add(filme("Drama " + i, 2000, null, null, drama));

        UserPreferences prefs = new UserPreferences();
        prefs.addLikedMovie(curtidoDrama);
        prefs.addLikedMovie(curtidoFaroeste);
        List<RecommendationResult> resultado = new RecommendationEngine(new MovieGraph(catalogo)).recommend(prefs, 5);

        assertEquals(outroFaroeste, resultado.get(0).getMovie());
    }

    @Test
    void mesmaEpocaSozinhaNaoConectaMais() {
        Movie a = filme("Filme A", 2021, new Director("Diretora A"), null);
        Movie b = filme("Filme B", 2024, new Director("Diretor B"), null);

        UserPreferences prefs = new UserPreferences();
        prefs.addLikedMovie(a);

        assertTrue(new RecommendationEngine(new MovieGraph(List.of(a, b))).recommend(prefs, 5).isEmpty());
    }

    @Test
    void notaDoTmdbDesempataConexoesIguais() {
        Director nolan = new Director("Christopher Nolan");
        Movie curtido = filme("Oppenheimer", 2023, nolan, null);
        Movie fraco = filme("Fraco", 2010, nolan, null);
        Movie forte = filme("Forte", 2010, nolan, null);
        fraco.setRating(5.0);
        forte.setRating(8.5);

        UserPreferences prefs = new UserPreferences();
        prefs.addLikedMovie(curtido);
        List<RecommendationResult> resultado = new RecommendationEngine(new MovieGraph(List.of(curtido, fraco, forte))).recommend(prefs, 5);

        assertEquals(forte, resultado.get(0).getMovie());
        assertEquals(fraco, resultado.get(1).getMovie());
    }

    @Test
    void diretoresComOMesmoNomeNaoSeMisturam() {
        Director umJohn = new Director("John Smith");
        umJohn.setId(1L);
        Director outroJohn = new Director("John Smith");
        outroJohn.setId(2L);
        Movie curtido = filme("Filme do Primeiro", 2020, umJohn, null);
        Movie doOutro = filme("Filme do Segundo", 2021, outroJohn, null);

        UserPreferences prefs = new UserPreferences();
        prefs.addLikedMovie(curtido);

        assertTrue(new RecommendationEngine(new MovieGraph(List.of(curtido, doOutro))).recommend(prefs, 5).isEmpty());
    }

    @Test
    void pontuacaoNaoDependeDaOrdemDosCurtidos() {
        Actor zendaya = new Actor("Zendaya");
        Movie duna = filme("Duna", 2021, null, null);
        Movie rivais = filme("Rivais", 2024, null, null);
        Movie alvo = filme("Alvo", 2025, null, null);
        duna.getCast().add(new MovieCast(duna, zendaya, RoleType.CAMEO));
        rivais.getCast().add(new MovieCast(rivais, zendaya, RoleType.PROTAGONIST));
        alvo.getCast().add(new MovieCast(alvo, zendaya, RoleType.PROTAGONIST));
        MovieGraph grafo = new MovieGraph(List.of(duna, rivais, alvo));

        UserPreferences umaOrdem = new UserPreferences();
        umaOrdem.addLikedMovie(duna);
        umaOrdem.addLikedMovie(rivais);
        UserPreferences outraOrdem = new UserPreferences();
        outraOrdem.addLikedMovie(rivais);
        outraOrdem.addLikedMovie(duna);

        assertEquals(new RecommendationEngine(grafo).recommend(umaOrdem, 5).get(0).getScore(),
                new RecommendationEngine(grafo).recommend(outraOrdem, 5).get(0).getScore());
    }

    @Test
    void devolveSoOsTopNEmOrdemDecrescente() {
        Genre drama = new Genre("Drama");
        Director nolan = new Director("Christopher Nolan");
        Movie curtido = filme("Curtido", 2020, nolan, null, drama);
        Movie doNolan = filme("Do Nolan", 2021, nolan, null, drama);
        List<Movie> catalogo = new java.util.ArrayList<>(List.of(curtido, doNolan));
        for (int i = 0; i < 30; i++) catalogo.add(filme("Drama " + i, 2000, null, null, drama));

        UserPreferences prefs = new UserPreferences();
        prefs.addLikedMovie(curtido);
        List<RecommendationResult> resultado = new RecommendationEngine(new MovieGraph(catalogo)).recommend(prefs, 3);

        assertEquals(3, resultado.size());
        assertEquals(doNolan, resultado.get(0).getMovie());
        for (int i = 1; i < resultado.size(); i++) {
            assertTrue(resultado.get(i - 1).getScore() >= resultado.get(i).getScore());
        }
    }

    @Test
    void parecidoComFilmeRejeitadoPerdePontos() {
        Director nolan = new Director("Christopher Nolan");
        Director outro = new Director("Outro Diretor");
        Genre drama = new Genre("Drama");
        Movie curtido = filme("Curtido", 2020, null, null, drama);
        Movie rejeitado = filme("Rejeitado", 2019, nolan, null);
        Movie doNolan = filme("Do Nolan", 2021, nolan, null, drama);
        Movie doOutro = filme("Do Outro", 2021, outro, null, drama);
        MovieGraph grafo = new MovieGraph(List.of(curtido, rejeitado, doNolan, doOutro));

        UserPreferences prefs = new UserPreferences();
        prefs.addLikedMovie(curtido);
        prefs.addDislikedMovie(rejeitado);
        List<RecommendationResult> resultado = new RecommendationEngine(grafo).recommend(prefs, 5);

        assertEquals(doOutro, resultado.get(0).getMovie());
        assertTrue(resultado.get(0).getScore() > resultado.get(1).getScore());
        assertTrue(resultado.stream().noneMatch(r -> r.getMovie().equals(rejeitado)));
    }

    @Test
    void sementeMaisPesadaPuxaMais() {
        Director a = new Director("Diretora A");
        Director b = new Director("Diretor B");
        Movie sementeA = filme("Semente A", 2020, a, null);
        Movie sementeB = filme("Semente B", 2020, b, null);
        Movie deA = filme("De A", 2021, a, null);
        Movie deB = filme("De B", 2021, b, null);

        UserPreferences prefs = new UserPreferences();
        prefs.addLikedMovie(sementeA, 0.6);
        prefs.addLikedMovie(sementeB, 1.2);
        List<RecommendationResult> resultado = new RecommendationEngine(new MovieGraph(List.of(sementeA, sementeB, deA, deB))).recommend(prefs, 5);

        assertEquals(deB, resultado.get(0).getMovie());
    }

    @Test
    void quemCurtiuTambemCurtiuConectaFilmesSemMetadadoEmComum() {
        Movie aftersun = filme("Aftersun", 2022, new Director("Charlotte Wells"), null);
        Movie pastLives = filme("Vidas Passadas", 2023, new Director("Celine Song"), null);
        Movie solto = filme("Solto", 2023, new Director("Outra Pessoa"), null);
        // três pessoas curtiram os dois; uma curtiu Aftersun e o solto
        Map<Long, List<Long>> curtidas = Map.of(
                10L, List.of(aftersun.getId(), pastLives.getId()),
                11L, List.of(aftersun.getId(), pastLives.getId()),
                12L, List.of(aftersun.getId(), pastLives.getId()),
                13L, List.of(aftersun.getId(), solto.getId()));
        MovieGraph grafo = new MovieGraph(List.of(aftersun, pastLives, solto), curtidas);

        UserPreferences prefs = new UserPreferences();
        prefs.addLikedMovie(aftersun);
        List<RecommendationResult> resultado = new RecommendationEngine(grafo).recommend(prefs, 5);

        assertEquals(1, resultado.size(), "uma pessoa só em comum não basta");
        assertEquals(pastLives, resultado.get(0).getMovie());
        assertEquals("quem curtiu Aftersun também curtiu", resultado.get(0).getReasons().get(0));
    }

    @Test
    void medicaoIgnoraAsCurtidasDaPropriaPessoa() {
        Movie a = filme("A", 2020, null, null);
        Movie b = filme("B", 2020, null, null);
        // só duas pessoas curtiram os dois; sem uma delas, sobra uma e a conexão some
        MovieGraph grafo = new MovieGraph(List.of(a, b), Map.of(
                10L, List.of(a.getId(), b.getId()),
                11L, List.of(a.getId(), b.getId())));

        UserPreferences prefs = new UserPreferences();
        prefs.addLikedMovie(a);
        assertEquals(1, new RecommendationEngine(grafo).recommend(prefs, 5).size());
        prefs.setIgnoredUserId(10L);
        assertTrue(new RecommendationEngine(grafo).recommend(prefs, 5).isEmpty());
    }

    @Test
    void catalogoGrandeRespondeRapido() {
        java.util.Random sorteio = new java.util.Random(42);
        List<Genre> generos = new java.util.ArrayList<>();
        for (int i = 0; i < 19; i++) generos.add(new Genre("Gênero " + i));
        List<Director> diretores = new java.util.ArrayList<>();
        for (int i = 0; i < 3000; i++) diretores.add(new Director("Diretor " + i));
        List<Actor> atores = new java.util.ArrayList<>();
        for (int i = 0; i < 20000; i++) atores.add(new Actor("Ator " + i));

        List<Movie> catalogo = new java.util.ArrayList<>();
        for (int i = 0; i < 20000; i++) {
            Movie m = filme("Filme " + i, 1950 + sorteio.nextInt(76), diretores.get(sorteio.nextInt(diretores.size())), null,
                    generos.get(0), generos.get(sorteio.nextInt(generos.size())), generos.get(sorteio.nextInt(generos.size())));
            for (int a = 0; a < 10; a++) {
                m.getCast().add(new MovieCast(m, atores.get(sorteio.nextInt(atores.size())), a < 3 ? RoleType.PROTAGONIST : RoleType.SUPPORTING));
            }
            catalogo.add(m);
        }
        // 3 mil pessoas com 80 curtidas cada, metade delas nos 2 mil filmes mais conhecidos
        Map<Long, List<Long>> curtidas = new java.util.HashMap<>();
        for (long u = 0; u < 3000; u++) {
            List<Long> ids = new java.util.ArrayList<>();
            for (int c = 0; c < 80; c++) {
                int indice = c % 2 == 0 ? sorteio.nextInt(2000) : sorteio.nextInt(catalogo.size());
                ids.add(catalogo.get(indice).getId());
            }
            curtidas.put(u, ids);
        }
        MovieGraph grafo = new MovieGraph(catalogo, curtidas);
        UserPreferences prefs = new UserPreferences();
        for (int i = 0; i < 5; i++) prefs.addLikedMovie(catalogo.get(i * 300));
        for (int i = 0; i < 10; i++) prefs.addDislikedMovie(catalogo.get(5000 + i));

        RecommendationEngine engine = new RecommendationEngine(grafo);
        for (int i = 0; i < 3; i++) engine.recommend(prefs, 100); // aquece o JIT
        long inicio = System.nanoTime();
        List<RecommendationResult> resultado = engine.recommend(prefs, 100);
        long ms = (System.nanoTime() - inicio) / 1_000_000;
        System.out.println("recomendação num catálogo de 20 mil filmes: " + ms + " ms");

        assertEquals(100, resultado.size());
        assertTrue(ms < 500, "demorou " + ms + " ms");
    }
}
