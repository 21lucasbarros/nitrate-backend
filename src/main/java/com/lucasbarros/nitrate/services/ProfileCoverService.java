package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.MovieBackdropsDTO;
import com.lucasbarros.nitrate.dto.ProfileCoverRequestDTO;
import com.lucasbarros.nitrate.dto.SeriesSearchResultDTO;
import com.lucasbarros.nitrate.dto.UserProfileDTO;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import com.lucasbarros.nitrate.tmdb.TmdbClient;
import com.lucasbarros.nitrate.tmdb.dto.TmdbMovieDetailDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbMovieImagesDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/** O fundo do topo do perfil: uma cena de qualquer filme do TMDB, escolhida pela própria pessoa. */
@Service
public class ProfileCoverService {

    private static final int MAX_CENAS = 60;
    private static final Duration VALIDADE = Duration.ofHours(24);

    @Autowired private TmdbClient tmdbClient;
    @Autowired private UserRepository userRepository;
    @Autowired private UserService userService;

    // a mesma lista é pedida para mostrar as cenas e de novo para conferir a escolhida
    private final Map<Long, CenasEmCache> cache = new ConcurrentHashMap<>();

    private record CenasEmCache(MovieBackdropsDTO cenas, Instant em) {}

    /** as cenas do filme, das sem texto e mais votadas para as outras */
    public MovieBackdropsDTO cenas(Long tmdbId) {
        CenasEmCache emCache = cache.get(tmdbId);
        if (emCache != null && emCache.em().plus(VALIDADE).isAfter(Instant.now())) {
            return emCache.cenas();
        }

        TmdbMovieDetailDTO detalhe;
        try {
            detalhe = tmdbClient.buscarDetalhesComImagens(tmdbId);
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Filme não encontrado no TMDB. Id: " + tmdbId);
        }

        List<String> caminhos = ordenarCenas(detalhe.getImages(), detalhe.getBackdropPath());

        String titulo = detalhe.getTitle() == null || detalhe.getTitle().isBlank() ? "Filme " + tmdbId : detalhe.getTitle();
        MovieBackdropsDTO resultado = new MovieBackdropsDTO(tmdbId, titulo, SeriesSearchResultDTO.ano(detalhe.getReleaseDate()), caminhos);
        cache.put(tmdbId, new CenasEmCache(resultado, Instant.now()));
        return resultado;
    }

    /**
     * Os caminhos das cenas, das sem texto e mais votadas para as outras (vale também para as séries). O
     * TMDB às vezes não lista a cena principal entre as imagens: ela entra no começo.
     */
    public static List<String> ordenarCenas(TmdbMovieImagesDTO imagens, String principal) {
        List<TmdbMovieImagesDTO.Image> lista = imagens == null ? List.of() : imagens.getBackdrops();
        List<String> caminhos = lista.stream()
                .filter(i -> i.getFilePath() != null)
                .sorted(Comparator
                        .comparing((TmdbMovieImagesDTO.Image i) -> i.getLanguage() != null) // sem texto primeiro
                        .thenComparing(i -> i.getVoteAverage() == null ? 0 : i.getVoteAverage(), Comparator.reverseOrder()))
                .map(TmdbMovieImagesDTO.Image::getFilePath)
                .distinct()
                .limit(MAX_CENAS)
                .toList();
        if (principal != null && !caminhos.contains(principal)) {
            caminhos = Stream.concat(Stream.of(principal), caminhos.stream()).limit(MAX_CENAS).toList();
        }
        return caminhos;
    }

    @Transactional
    public UserProfileDTO salvar(User user, ProfileCoverRequestDTO dados) {
        if (dados.tmdbId() == null || dados.path() == null) {
            throw new IllegalArgumentException("Escolha um filme e uma cena.");
        }
        MovieBackdropsDTO cenas = cenas(dados.tmdbId());
        // só aceita uma cena que é mesmo deste filme: o caminho nunca vem livre do cliente
        String caminho = cenas.backdrops().stream()
                .filter(c -> Objects.equals(c, dados.path()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Essa cena não é deste filme."));

        // o principal do token é uma cópia desanexada: atualiza a linha do banco
        User salvo = userRepository.findById(user.getId()).orElseThrow();
        salvo.setCoverPath(caminho);
        salvo.setCoverTmdbId(cenas.tmdbId());
        salvo.setCoverTitle(cenas.title().length() > 300 ? cenas.title().substring(0, 300) : cenas.title());
        salvo.setCoverYear(cenas.year());
        return userService.perfil(userRepository.save(salvo));
    }

    /** volta para a cena do primeiro filme favorito */
    @Transactional
    public UserProfileDTO remover(User user) {
        User salvo = userRepository.findById(user.getId()).orElseThrow();
        salvo.setCoverPath(null);
        salvo.setCoverTmdbId(null);
        salvo.setCoverTitle(null);
        salvo.setCoverYear(null);
        return userService.perfil(userRepository.save(salvo));
    }
}
