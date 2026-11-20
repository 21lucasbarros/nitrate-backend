package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.FilmDTO;
import com.lucasbarros.nitrate.dto.PageDTO;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

// aba "Filmes" do perfil: um item por filme, ordenado em memória (sem banco)
@ExtendWith(MockitoExtension.class)
class UserServiceFilmesTest {

    @Mock private UserRepository userRepository;
    @Mock private ReviewRepository reviewRepository;
    @InjectMocks private UserService userService;

    private final User user = new User("Ana", "ana", "ana@x.com", "hash");
    private final List<Review> diario = new ArrayList<>();
    private long proximoId = 1;

    private Movie filme(String titulo, LocalDate estreia, Integer ano) {
        Movie m = new Movie(titulo, ano, null, null, null);
        m.setId(proximoId++);
        m.setReleaseDate(estreia);
        return m;
    }

    private Review registro(Movie filme, Double nota, boolean curtido, LocalDate assistidoEm) {
        Review r = new Review(user, filme);
        r.setId(proximoId++);
        r.setRating(nota);
        r.setLiked(curtido);
        r.setWatchedOn(assistidoEm);
        r.setCreatedAt(Instant.parse("2026-01-01T00:00:00Z"));
        diario.add(r);
        return r;
    }

    private final Movie duna = filme("Duna", LocalDate.of(2021, 10, 21), 2021);
    private final Movie alien = filme("Alien", LocalDate.of(1979, 9, 6), 1979);
    private final Movie oppenheimer = filme("Oppenheimer", LocalDate.of(2023, 7, 20), 2023);
    private final Movie semData = filme("Sem Data", null, null);
    private final Movie soAno = filme("Só Ano", null, 2000);

    @BeforeEach
    void montarDiario() {
        when(userRepository.findFirstByUsernameIgnoreCase("ana")).thenReturn(Optional.of(user));
        registro(duna, 3.0, false, LocalDate.of(2026, 1, 1));
        registro(duna, 4.5, true, LocalDate.of(2026, 6, 1)); // reassistido: este é o mais recente
        registro(alien, 5.0, true, LocalDate.of(2026, 3, 1));
        registro(oppenheimer, null, false, LocalDate.of(2026, 5, 1));
        registro(semData, 2.0, false, LocalDate.of(2026, 2, 1));
        registro(soAno, 4.0, false, LocalDate.of(2026, 4, 1));
        when(reviewRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(diario);
    }

    private List<String> titulos(String ordem, boolean soCurtidos) {
        PageDTO<FilmDTO> pagina = userService.filmes("ana", 0, 48, ordem, soCurtidos);
        return pagina.items().stream().map(f -> f.review().getMovie().getTitle()).toList();
    }

    @Test
    void padraoEDoLancamentoMaisNovoParaOMaisAntigoSemRepetirReassistidos() {
        assertEquals(List.of("Oppenheimer", "Duna", "Só Ano", "Alien", "Sem Data"), titulos(null, false));
    }

    @Test
    void reassistidoMostraORegistroMaisRecenteETodasAsVezes() {
        FilmDTO dunaItem = userService.filmes("ana", 0, 48, "lancamento", false).items().get(1);
        assertEquals(4.5, dunaItem.review().getRating());
        assertEquals(List.of(LocalDate.of(2026, 6, 1), LocalDate.of(2026, 1, 1)),
                dunaItem.viewings().stream().map(FilmDTO.Viewing::watchedOn).toList());
    }

    @Test
    void filmeVistoUmaVezTemUmRegistro() {
        FilmDTO alienItem = userService.filmes("ana", 0, 48, "lancamento", false).items().get(3);
        assertEquals("Alien", alienItem.review().getMovie().getTitle());
        assertEquals(1, alienItem.viewings().size());
    }

    @Test
    void lancamentoAntigos() {
        assertEquals(List.of("Alien", "Só Ano", "Duna", "Oppenheimer", "Sem Data"), titulos("lancamento-antigos", false));
    }

    @Test
    void maiorNotaPrimeiroESemNotaNoFim() {
        assertEquals(List.of("Alien", "Duna", "Só Ano", "Sem Data", "Oppenheimer"), titulos("nota", false));
    }

    @Test
    void menorNotaPrimeiroESemNotaNoFim() {
        assertEquals(List.of("Sem Data", "Só Ano", "Duna", "Alien", "Oppenheimer"), titulos("nota-menor", false));
    }

    @Test
    void assistidosRecentemente() {
        assertEquals(List.of("Duna", "Oppenheimer", "Só Ano", "Alien", "Sem Data"), titulos("assistidos", false));
    }

    @Test
    void soCurtidos() {
        assertEquals(List.of("Duna", "Alien"), titulos("lancamento", true));
    }

    @Test
    void ordemDesconhecidaCaiNoPadrao() {
        assertEquals(titulos("lancamento", false), titulos("qualquer-coisa", false));
    }

    @Test
    void paginacao() {
        PageDTO<FilmDTO> segunda = userService.filmes("ana", 1, 2, "lancamento", false);
        assertEquals(5, segunda.totalItems());
        assertEquals(3, segunda.totalPages());
        assertEquals(List.of("Só Ano", "Alien"), segunda.items().stream().map(f -> f.review().getMovie().getTitle()).toList());
    }
}
