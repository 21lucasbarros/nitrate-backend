package com.lucasbarros.nitrate.taste;

import com.lucasbarros.nitrate.entities.Director;
import com.lucasbarros.nitrate.entities.Genre;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.taste.TasteMatcher.Preferencias;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

// questionário de gostos: filtros das respostas e a grade de filmes (sem banco)
@ExtendWith(MockitoExtension.class)
class TasteMatcherTest {

    @Mock private ReviewRepository reviewRepository;
    // sem stub, como o TMDB fora do ar: os filtros com palavra-chave usam o plano B
    @Mock private PalavrasChave palavrasChave;
    @InjectMocks private TasteMatcher matcher;

    private long proximoId = 1;

    private Movie filme(String titulo, int ano, double nota, String diretor, String... generos) {
        Movie m = new Movie(titulo, ano, nota, diretor == null ? null : new Director(diretor), null);
        m.setId(proximoId++);
        m.setPosterPath("/" + titulo + ".jpg");
        // nomes como vêm do TMDB, com maiúsculas e acentos. O Genre compara pelo id: sem ele,
        // o HashSet guardaria um gênero só
        m.setGenres(Arrays.stream(generos).map(this::genero).collect(Collectors.toCollection(HashSet::new)));
        return m;
    }

    private long proximoGenero = 1;

    private Genre genero(String nome) {
        Genre g = new Genre(nome);
        g.setId(proximoGenero++);
        return g;
    }

    private Preferencias prefs(List<String> generos, List<String> respostas, List<String> epocas) {
        return Preferencias.de(generos, respostas, epocas);
    }

    @Test
    void comediaRomanticaPedeOsDoisGeneros() {
        Movie comRom = filme("Simplesmente Amor", 2003, 7.6, null, "Comédia", "Romance");
        Movie soRomance = filme("Titanic", 1997, 7.9, null, "Drama", "Romance");
        TasteQuiz.Filtro filtro = TasteQuiz.OPCAO_POR_ID.get("ROMANCE_COMEDIA").filtro();

        assertTrue(filtro.aceita(comRom, PalavrasChave.NENHUMA));
        assertFalse(filtro.aceita(soRomance, PalavrasChave.NENHUMA));
    }

    @Test
    void romanceQueFazChorarExcluiComedia() {
        Movie drama = filme("Titanic", 1997, 7.9, null, "Drama", "Romance");
        Movie dramedia = filme("Questão de Tempo", 2013, 7.9, null, "Drama", "Romance", "Comédia");
        TasteQuiz.Filtro filtro = TasteQuiz.OPCAO_POR_ID.get("ROMANCE_DRAMA").filtro();

        assertTrue(filtro.aceita(drama, PalavrasChave.NENHUMA));
        assertFalse(filtro.aceita(dramedia, PalavrasChave.NENHUMA));
    }

    @Test
    void ficcaoCientificaCasaSemDiferenciarMaiusculas() {
        Movie interestelar = filme("Interestelar", 2014, 8.4, null, "Ficção Científica", "Aventura");
        assertTrue(TasteQuiz.OPCAO_POR_ID.get("FICCAO_ESPACO").filtro().aceita(interestelar, PalavrasChave.NENHUMA));
    }

    @Test
    void idsDesconhecidosSaoIgnorados() {
        Preferencias p = prefs(List.of("ROMANCE", "NAO_EXISTE"), List.of("INVENTADA"), List.of("RECENTES", "x"));
        assertEquals(Set.of("ROMANCE"), p.genres());
        assertTrue(p.answers().isEmpty());
        assertEquals(Set.of("RECENTES"), p.eras());
    }

    @Test
    void aRespostaSubstituiOGenero() {
        // escolheu Romance e respondeu "comédia romântica": drama romântico não entra na grade
        Movie comRom = filme("Simplesmente Amor", 2003, 7.6, null, "Comédia", "Romance");
        Movie drama = filme("Titanic", 1997, 7.9, null, "Drama", "Romance");
        Movie terror = filme("Hereditário", 2018, 7.3, null, "Terror");
        lenient().when(reviewRepository.countPorFilme()).thenReturn(List.of());

        List<Movie> grade = matcher.melhores(
                prefs(List.of("ROMANCE"), List.of("ROMANCE_COMEDIA"), List.of()),
                List.of(drama, terror, comRom), Set.of(), 10);

        assertEquals(List.of(comRom), grade);
    }

    @Test
    void generoSemRespostaGanhaFilaPropria() {
        // escolheu Comédia (respondeu) e Romance (não respondeu): romance continua com fila
        Movie comRom = filme("Simplesmente Amor", 2003, 7.6, null, "Comédia", "Romance");
        Movie drama = filme("Titanic", 1997, 7.9, null, "Drama", "Romance");
        Movie acao = filme("Kingsman", 2015, 7.6, null, "Comédia", "Ação", "Crime");
        lenient().when(reviewRepository.countPorFilme()).thenReturn(List.of());

        List<TasteMatcher.Grupo> grupos = matcher.grupos(
                prefs(List.of("COMEDIA", "ROMANCE"), List.of("COMEDIA_ROMANTICA"), List.of()),
                List.of(comRom, drama, acao), Set.of(), 10);

        assertEquals(List.of("Comédia romântica", "Romance"), grupos.stream().map(TasteMatcher.Grupo::label).toList());
        assertEquals(List.of(comRom), grupos.get(0).filmes());
        assertEquals(List.of(drama), grupos.get(1).filmes());
    }

    @Test
    void comediaRomanticaNaoTrazCrimeNemAcao() {
        // o TMDB marca Pulp Fiction como crime, comédia e suspense
        Movie pulp = filme("Pulp Fiction", 1994, 8.5, null, "Crime", "Comédia", "Thriller", "Romance");
        assertFalse(TasteQuiz.OPCAO_POR_ID.get("COMEDIA_ROMANTICA").filtro().aceita(pulp, PalavrasChave.NENHUMA));
        assertFalse(TasteQuiz.OPCAO_POR_ID.get("ROMANCE_COMEDIA").filtro().aceita(pulp, PalavrasChave.NENHUMA));
    }

    @Test
    void comediaRomanticaPuraVemAntesDoDramaComRomance() {
        // nota mais alta, mas com drama e fantasia a mais: perde para a comédia romântica pura
        Movie dramedia = filme("Questão de Tempo", 2013, 8.0, null, "Comédia", "Romance", "Drama", "Fantasia");
        Movie pura = filme("O Amor Não Tira Férias", 2006, 7.1, null, "Comédia", "Romance");
        lenient().when(reviewRepository.countPorFilme()).thenReturn(List.of());

        List<Movie> grade = matcher.melhores(
                prefs(List.of("COMEDIA"), List.of("COMEDIA_ROMANTICA"), List.of()), List.of(dramedia, pura), Set.of(), 10);

        assertEquals(List.of(pura, dramedia), grade);
    }

    @Test
    void rodizioDaEspacoACadaResposta() {
        List<Movie> filmes = new ArrayList<>();
        // muitos de terror psicológico, um só de comédia romântica: ela não pode ficar de fora
        for (int i = 0; i < 6; i++) filmes.add(filme("Terror " + i, 2018, 8.0, "Diretor " + i, "Terror", "Thriller"));
        Movie comRom = filme("Simplesmente Amor", 2003, 6.0, null, "Comédia", "Romance");
        filmes.add(comRom);
        lenient().when(reviewRepository.countPorFilme()).thenReturn(List.of());

        List<Movie> grade = matcher.melhores(
                prefs(List.of(), List.of("TERROR_PSICOLOGICO", "ROMANCE_COMEDIA"), List.of()), filmes, Set.of(), 3);

        assertEquals(3, grade.size());
        assertTrue(grade.contains(comRom));
    }

    @Test
    void noMaximoDoisDoMesmoDiretor() {
        List<Movie> filmes = new ArrayList<>();
        for (int i = 0; i < 5; i++) filmes.add(filme("Nolan " + i, 2010, 8.5, "Christopher Nolan", "Ficção científica", "Ação"));
        lenient().when(reviewRepository.countPorFilme()).thenReturn(List.of());

        List<Movie> grade = matcher.melhores(prefs(List.of(), List.of("FICCAO_ACAO"), List.of()), filmes, Set.of(), 10);

        assertEquals(2, grade.size());
    }

    @Test
    void foraDaEpocaFicaAtras() {
        Movie classico = filme("Casablanca", 1942, 8.5, null, "Drama", "Romance");
        Movie recente = filme("Vidas Passadas", 2023, 7.8, null, "Drama", "Romance");
        lenient().when(reviewRepository.countPorFilme()).thenReturn(List.of());

        List<Movie> grade = matcher.melhores(
                prefs(List.of("ROMANCE"), List.of(), List.of("RECENTES")), List.of(classico, recente), Set.of(), 10);

        assertEquals(List.of(recente, classico), grade);
    }

    @Test
    void filmeSemTituloTraduzidoFicaAtras() {
        Movie japones = filme("愛のぬくもり", 2024, 7.5, null, "Romance");
        Movie conhecido = filme("Antes do Amanhecer", 1995, 7.0, null, "Romance", "Drama");
        lenient().when(reviewRepository.countPorFilme()).thenReturn(List.of());

        List<Movie> grade = matcher.melhores(prefs(List.of("ROMANCE"), List.of(), List.of()),
                List.of(japones, conhecido), Set.of(), 10);

        assertEquals(List.of(conhecido, japones), grade);
    }

    @Test
    void musicalUsaAPalavraChaveDoTmdbENaoOGeneroMusica() {
        // La La Land não tem o gênero "Música" no TMDB; Bohemian Rhapsody tem, mas não é musical
        Movie laLaLand = filme("La La Land", 2016, 7.9, null, "Romance", "Drama", "Comédia");
        laLaLand.setTmdbId(313369L);
        Movie bohemian = filme("Bohemian Rhapsody", 2018, 8.0, null, "Música", "Drama", "História");
        bohemian.setTmdbId(424694L);
        when(palavrasChave.disponivel(PalavrasChave.MUSICAL)).thenReturn(true);
        // só La La Land está na lista de musicais do TMDB
        when(palavrasChave.tem(any(Movie.class), eq(PalavrasChave.MUSICAL)))
                .thenAnswer(chamada -> chamada.getArgument(0) == laLaLand);
        lenient().when(reviewRepository.countPorFilme()).thenReturn(List.of());

        List<Movie> grade = matcher.melhores(prefs(List.of("MUSICA"), List.of(), List.of()),
                List.of(bohemian, laLaLand), Set.of(), 10);

        assertEquals(List.of(laLaLand), grade);
    }

    @Test
    void semAListaDoTmdbMusicalVoltaAoGeneroMusica() {
        Movie bohemian = filme("Bohemian Rhapsody", 2018, 8.0, null, "Música", "Drama", "História");
        lenient().when(reviewRepository.countPorFilme()).thenReturn(List.of());

        List<Movie> grade = matcher.melhores(prefs(List.of("MUSICA"), List.of(), List.of()),
                List.of(bohemian), Set.of(), 10);

        assertEquals(List.of(bohemian), grade);
    }

    @Test
    void cinebiografiaMusicalUsaOGeneroMusica() {
        Movie bohemian = filme("Bohemian Rhapsody", 2018, 8.0, null, "Música", "Drama", "História");
        Movie show = filme("Stop Making Sense", 1984, 8.3, null, "Música", "Documentário");
        TasteQuiz.Filtro filtro = TasteQuiz.OPCAO_POR_ID.get("MUSICA_BIOGRAFIA").filtro();

        assertTrue(filtro.aceita(bohemian, palavrasChave));
        assertFalse(filtro.aceita(show, palavrasChave));
    }

    @Test
    void motivosDizemQualRespostaCombinou() {
        Movie comRom = filme("Simplesmente Amor", 2003, 7.6, null, "Comédia", "Romance");
        Preferencias p = prefs(List.of("ROMANCE", "COMEDIA"), List.of("ROMANCE_COMEDIA"), List.of());

        assertEquals(List.of("comédia romântica"), matcher.motivos(p, comRom));
    }
}
