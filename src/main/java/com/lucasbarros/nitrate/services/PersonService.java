package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.PersonCreditDTO;
import com.lucasbarros.nitrate.dto.PersonDetailDTO;
import com.lucasbarros.nitrate.dto.PersonSummaryDTO;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.tmdb.TmdbClient;
import com.lucasbarros.nitrate.tmdb.dto.TmdbPersonCreditDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbPersonDetailDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbPersonSearchResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;

// atores e diretores vêm direto do TMDB; não guardamos pessoas no banco
@Service
public class PersonService {

    private static final int TERMO_MIN = 2;
    private static final int MAX_CREDITOS = 100;
    private static final double POPULARIDADE_MINIMA = 1.0; // abaixo disso são homônimos obscuros

    @Autowired private TmdbClient tmdbClient;
    @Autowired private MovieRepository movieRepository;

    public List<PersonSummaryDTO> pesquisar(String termo) {
        List<PersonSummaryDTO> resultados = new ArrayList<>();
        if (termo == null || termo.trim().length() < TERMO_MIN) {
            return resultados;
        }
        for (TmdbPersonSearchResponseDTO.Result pessoa : tmdbClient.pesquisarPessoas(termo.trim()).getResults()) {
            // não filtramos por departamento: muitos diretores (ex.: James Gunn) estão como "Writing".
            // descartamos homônimos obscuros (pouca popularidade no TMDB)
            if (pessoa.getPopularity() >= POPULARIDADE_MINIMA) {
                resultados.add(new PersonSummaryDTO(pessoa));
            }
        }
        return resultados;
    }

    public PersonDetailDTO detalhe(Long personId) {
        TmdbPersonDetailDTO pessoa = tmdbClient.buscarPessoaComFilmografia(personId);

        List<TmdbPersonCreditDTO> dirigidos = new ArrayList<>();
        for (TmdbPersonCreditDTO credito : pessoa.getMovieCredits().getCrew()) {
            if ("Director".equals(credito.getJob())) dirigidos.add(credito);
        }
        List<TmdbPersonCreditDTO> atuados = new ArrayList<>();
        for (TmdbPersonCreditDTO credito : pessoa.getMovieCredits().getCast()) {
            if (!ehAparicaoComoSiMesmo(credito)) atuados.add(credito);
        }

        dirigidos = maisConhecidos(dirigidos, MAX_CREDITOS);
        atuados = maisConhecidos(atuados, MAX_CREDITOS);

        // marca com o id local os filmes que já estão no banco (abrem direto)
        Set<Long> tmdbIds = new HashSet<>();
        for (TmdbPersonCreditDTO c : dirigidos) tmdbIds.add(c.getId());
        for (TmdbPersonCreditDTO c : atuados) tmdbIds.add(c.getId());
        Map<Long, Long> idLocalPorTmdbId = new HashMap<>();
        for (Movie filme : movieRepository.findByTmdbIdIn(tmdbIds)) {
            idLocalPorTmdbId.put(filme.getTmdbId(), filme.getId());
        }

        return new PersonDetailDTO(pessoa, paraDTO(dirigidos, idLocalPorTmdbId), paraDTO(atuados, idLocalPorTmdbId));
    }

    /**
     * Filmes mais conhecidos de uma pessoa, já lançados e com votos suficientes. Usado para
     * expandir o grafo: como ator, só papéis de destaque (topo dos créditos).
     */
    public List<Long> destaques(Long personId, boolean comoDiretor, int limite, int votosMinimos) {
        TmdbPersonDetailDTO pessoa = tmdbClient.buscarPessoaComFilmografia(personId);
        String hoje = LocalDate.now().toString();

        List<TmdbPersonCreditDTO> candidatos = new ArrayList<>();
        List<TmdbPersonCreditDTO> creditos = comoDiretor
                ? pessoa.getMovieCredits().getCrew()
                : pessoa.getMovieCredits().getCast();
        for (TmdbPersonCreditDTO credito : creditos) {
            if (comoDiretor && !"Director".equals(credito.getJob())) continue;
            if (!comoDiretor && (credito.getOrder() == null || credito.getOrder() > 4 || ehAparicaoComoSiMesmo(credito))) continue;
            if (credito.getVoteCount() < votosMinimos) continue;
            String data = credito.getReleaseDate();
            if (data == null || data.isEmpty() || data.compareTo(hoje) > 0) continue; // ainda não lançado
            candidatos.add(credito);
        }

        List<Long> ids = new ArrayList<>();
        for (TmdbPersonCreditDTO credito : maisConhecidos(candidatos, limite)) {
            ids.add(credito.getId());
        }
        return ids;
    }

    // ordena por número de votos (popularidade duradoura) e remove repetidos
    private List<TmdbPersonCreditDTO> maisConhecidos(List<TmdbPersonCreditDTO> creditos, int limite) {
        List<TmdbPersonCreditDTO> ordenados = new ArrayList<>(creditos);
        ordenados.sort(Comparator.comparingInt(TmdbPersonCreditDTO::getVoteCount).reversed());
        List<TmdbPersonCreditDTO> unicos = new ArrayList<>();
        Set<Long> vistos = new HashSet<>();
        for (TmdbPersonCreditDTO credito : ordenados) {
            if (vistos.add(credito.getId())) unicos.add(credito);
            if (unicos.size() == limite) break;
        }
        return unicos;
    }

    // documentários e talk shows aparecem como "Self", "Himself"...
    private boolean ehAparicaoComoSiMesmo(TmdbPersonCreditDTO credito) {
        String personagem = credito.getCharacter();
        if (personagem == null) return false;
        String p = personagem.toLowerCase();
        return p.contains("self") || p.contains("himself") || p.contains("herself") || p.contains("ele mesmo") || p.contains("ela mesma");
    }

    private List<PersonCreditDTO> paraDTO(List<TmdbPersonCreditDTO> creditos, Map<Long, Long> idLocalPorTmdbId) {
        List<PersonCreditDTO> dtos = new ArrayList<>();
        for (TmdbPersonCreditDTO credito : creditos) {
            dtos.add(new PersonCreditDTO(credito, idLocalPorTmdbId.get(credito.getId())));
        }
        return dtos;
    }
}
