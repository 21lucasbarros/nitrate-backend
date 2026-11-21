package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.PersonCreditDTO;
import com.lucasbarros.nitrate.dto.PersonDetailDTO;
import com.lucasbarros.nitrate.dto.PersonSummaryDTO;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import com.lucasbarros.nitrate.tmdb.TmdbClient;
import com.lucasbarros.nitrate.tmdb.dto.TmdbPersonCreditDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbPersonDetailDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbPersonSearchResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// atores e diretores vêm direto do TMDB; não guardamos pessoas no banco
@Service
public class PersonService {

    private static final int TERMO_MIN = 2;
    private static final int MAX_CREDITOS = 100;
    private static final double POPULARIDADE_MINIMA = 1.0; // abaixo disso são homônimos obscuros

    @Autowired private TmdbClient tmdbClient;
    @Autowired private MovieRepository movieRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private PersonSlugService personSlugService;

    // filmografias mudam pouco e são lidas pela página, pelas conquistas e pelo grafo: uma consulta
    // ao TMDB a cada poucas horas basta
    private static final Duration VALIDADE_FILMOGRAFIA = Duration.ofHours(6);
    private final Map<Long, FilmografiaEmCache> cacheFilmografia = new ConcurrentHashMap<>();

    private record FilmografiaEmCache(TmdbPersonDetailDTO pessoa, Instant em) {}

    /** filmes que entram na conta da filmografia: os que a página mostra, direção e atuação */
    public record Filmografia(TmdbPersonDetailDTO pessoa, List<TmdbPersonCreditDTO> dirigidos, List<TmdbPersonCreditDTO> atuados) {

        /** sem repetir quem dirigiu e atuou no mesmo filme */
        public Set<Long> tmdbIds() {
            Set<Long> ids = new LinkedHashSet<>();
            dirigidos.forEach(c -> ids.add(c.getId()));
            atuados.forEach(c -> ids.add(c.getId()));
            return ids;
        }

        /** só os já lançados: filme anunciado não dá para ter visto */
        public Set<Long> lancados() {
            String hoje = LocalDate.now().toString();
            Set<Long> ids = new LinkedHashSet<>();
            for (List<TmdbPersonCreditDTO> creditos : List.of(dirigidos, atuados)) {
                for (TmdbPersonCreditDTO c : creditos) {
                    String data = c.getReleaseDate();
                    if (data != null && !data.isEmpty() && data.compareTo(hoje) <= 0) ids.add(c.getId());
                }
            }
            return ids;
        }
    }

    public List<PersonSummaryDTO> pesquisar(String termo) {
        List<PersonSummaryDTO> resultados = new ArrayList<>();
        if (termo == null || termo.trim().length() < TERMO_MIN) {
            return resultados;
        }
        List<TmdbPersonSearchResponseDTO.Result> relevantes = new ArrayList<>();
        for (TmdbPersonSearchResponseDTO.Result pessoa : tmdbClient.pesquisarPessoas(termo.trim()).getResults()) {
            // não filtramos por departamento: muitos diretores (ex.: James Gunn) estão como "Writing".
            if (relevante(pessoa)) relevantes.add(pessoa);
        }
        Map<Long, String> nomes = new HashMap<>();
        relevantes.forEach(p -> nomes.put(p.getId(), p.getName()));
        Map<Long, String> slugs = personSlugService.slugs(nomes);
        for (TmdbPersonSearchResponseDTO.Result pessoa : relevantes) {
            resultados.add(new PersonSummaryDTO(pessoa, slugs.get(pessoa.getId())));
        }
        return resultados;
    }

    /**
     * Descarta homônimos obscuros. Só a popularidade não basta: na escala atual do TMDB muita
     * gente conhecida fica abaixo do mínimo (ex.: Ingrid Guimarães), então quem tem foto e
     * algum filme no "conhecido por" também entra.
     */
    private boolean relevante(TmdbPersonSearchResponseDTO.Result pessoa) {
        if (pessoa.getPopularity() >= POPULARIDADE_MINIMA) return true;
        if (pessoa.getProfilePath() == null) return false;
        for (TmdbPersonSearchResponseDTO.KnownFor obra : pessoa.getKnownFor()) {
            if ("movie".equals(obra.getMediaType())) return true;
        }
        return false;
    }

    /** `ref`: o slug ("ben-stiller") ou o id do TMDB (links antigos e o app) */
    public Long resolver(String ref) {
        if (ref != null && ref.matches("\\d{1,18}")) return Long.valueOf(ref);
        return personSlugService.tmdbIdDe(Objects.requireNonNullElse(ref, "").trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ResourceNotFoundException("Pessoa não encontrada: " + ref));
    }

    public PersonDetailDTO detalhe(String ref) {
        Filmografia filmografia = filmografia(resolver(ref));
        TmdbPersonDetailDTO pessoa = filmografia.pessoa();
        List<TmdbPersonCreditDTO> dirigidos = filmografia.dirigidos();
        List<TmdbPersonCreditDTO> atuados = filmografia.atuados();

        // marca com o id local os filmes que já estão no banco (abrem direto)
        Set<Long> tmdbIds = filmografia.tmdbIds();
        Map<Long, Movie> localPorTmdbId = new HashMap<>();
        Map<Long, Object[]> mediaPorTmdbId = new HashMap<>();
        if (!tmdbIds.isEmpty()) {
            for (Movie filme : movieRepository.findByTmdbIdIn(tmdbIds)) {
                localPorTmdbId.put(filme.getTmdbId(), filme);
            }
            // nota média no Nitrate, para ordenar a filmografia pelos mais bem avaliados
            for (Object[] linha : reviewRepository.findMediasPorTmdbId(tmdbIds)) {
                mediaPorTmdbId.put((Long) linha[0], linha);
            }
        }

        return new PersonDetailDTO(pessoa, personSlugService.slug(pessoa.getId(), pessoa.getName()),
                paraDTO(dirigidos, localPorTmdbId, mediaPorTmdbId),
                paraDTO(atuados, localPorTmdbId, mediaPorTmdbId));
    }

    /** direção e atuação como a página mostra: sem aparições como si mesmo, os mais conhecidos primeiro */
    public Filmografia filmografia(Long personId) {
        TmdbPersonDetailDTO pessoa = pessoaComFilmografia(personId);
        if (pessoa == null || pessoa.getId() == null) {
            throw new ResourceNotFoundException("Pessoa não encontrada. Id: " + personId);
        }
        List<TmdbPersonCreditDTO> dirigidos = new ArrayList<>();
        for (TmdbPersonCreditDTO credito : pessoa.getMovieCredits().getCrew()) {
            if ("Director".equals(credito.getJob())) dirigidos.add(credito);
        }
        List<TmdbPersonCreditDTO> atuados = new ArrayList<>();
        for (TmdbPersonCreditDTO credito : pessoa.getMovieCredits().getCast()) {
            if (!ehAparicaoComoSiMesmo(credito)) atuados.add(credito);
        }
        return new Filmografia(pessoa, maisConhecidos(dirigidos, MAX_CREDITOS), maisConhecidos(atuados, MAX_CREDITOS));
    }

    private TmdbPersonDetailDTO pessoaComFilmografia(Long personId) {
        FilmografiaEmCache emCache = cacheFilmografia.get(personId);
        if (emCache != null && emCache.em().plus(VALIDADE_FILMOGRAFIA).isAfter(Instant.now())) {
            return emCache.pessoa();
        }
        TmdbPersonDetailDTO pessoa = tmdbClient.buscarPessoaComFilmografia(personId);
        if (pessoa != null) cacheFilmografia.put(personId, new FilmografiaEmCache(pessoa, Instant.now()));
        return pessoa;
    }

    /**
     * Filmes mais conhecidos de uma pessoa, já lançados e com votos suficientes. Usado para
     * expandir o grafo: como ator, só papéis de destaque (topo dos créditos).
     */
    public List<Long> destaques(Long personId, boolean comoDiretor, int limite, int votosMinimos) {
        TmdbPersonDetailDTO pessoa = pessoaComFilmografia(personId);
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

    private List<PersonCreditDTO> paraDTO(List<TmdbPersonCreditDTO> creditos, Map<Long, Movie> localPorTmdbId,
                                          Map<Long, Object[]> mediaPorTmdbId) {
        List<PersonCreditDTO> dtos = new ArrayList<>();
        for (TmdbPersonCreditDTO credito : creditos) {
            Object[] media = mediaPorTmdbId.get(credito.getId());
            dtos.add(new PersonCreditDTO(credito, localPorTmdbId.get(credito.getId()),
                    media == null ? null : (Double) media[1],
                    media == null ? 0 : (Long) media[2]));
        }
        return dtos;
    }
}
