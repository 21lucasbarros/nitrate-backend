package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.tmdb.dto.TmdbMovieSummaryDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class LetterboxdImportServiceTest {

    private TmdbMovieSummaryDTO filme(long id, String titulo, String original, int votos) {
        TmdbMovieSummaryDTO f = new TmdbMovieSummaryDTO();
        f.setId(id);
        f.setTitle(titulo);
        f.setOriginalTitle(original);
        f.setVoteCount(votos);
        return f;
    }

    @Test
    void prefereOTituloExatoMaisVotadoAoPrimeiroResultado() {
        // busca real por "Obsession" em 2025: o do Curry Barker vem traduzido ("Obsessão")
        // e um homônimo obscuro com 1 voto também casa o título
        List<TmdbMovieSummaryDTO> candidatos = List.of(
                filme(1436161, "Obsession", "Obsession", 1),
                filme(1339713, "Obsessão", "Obsession", 6641),
                filme(1688662, "Sharp Obsession", "Sharp Obsession", 1));
        assertEquals(1339713L, LetterboxdImportService.melhorCandidato("Obsession", candidatos).getId());
    }

    @Test
    void tituloExatoVenceTituloParecidoMaisFamoso() {
        List<TmdbMovieSummaryDTO> candidatos = List.of(
                filme(1, "Obsessão Secreta", "Secret Obsession", 1221),
                filme(2, "Obsession", "Obsession", 3));
        assertEquals(2L, LetterboxdImportService.melhorCandidato("Obsession", candidatos).getId());
    }

    @Test
    void ignoraAcentosEPontuacaoNoTitulo() {
        List<TmdbMovieSummaryDTO> candidatos = List.of(
                filme(1, "Outro Filme", "Another", 500),
                filme(2, "O Fabuloso Destino de Amélie Poulain", "Le Fabuleux Destin d'Amélie Poulain", 10));
        assertEquals(2L, LetterboxdImportService.melhorCandidato("Le fabuleux destin d'Amelie Poulain", candidatos).getId());
    }

    @Test
    void semTituloExatoMantemAOrdemDoTmdbEntreEmpatados() {
        List<TmdbMovieSummaryDTO> candidatos = List.of(
                filme(1, "A", "A", 10),
                filme(2, "B", "B", 10));
        assertEquals(1L, LetterboxdImportService.melhorCandidato("C", candidatos).getId());
        assertNull(LetterboxdImportService.melhorCandidato("C", List.of()));
    }
}
