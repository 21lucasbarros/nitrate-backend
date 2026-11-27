package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.PostersDTO;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Series;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.repositories.SeriesRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import com.lucasbarros.nitrate.tmdb.TmdbClient;
import com.lucasbarros.nitrate.tmdb.dto.TmdbMovieDetailDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbMovieImagesDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

/** Os pôsteres alternativos de um filme ou de uma série no TMDB, para a pessoa trocar o pôster nas reviews dela. */
@Service
public class PosterService {

    private static final int MAX_POSTERES = 60;
    private static final Duration VALIDADE = Duration.ofHours(24);

    @Autowired private TmdbClient tmdbClient;
    @Autowired private MovieRepository movieRepository;
    @Autowired private SeriesRepository seriesRepository;

    // a mesma lista é pedida para mostrar os pôsteres e de novo para conferir o escolhido
    private final Map<String, PosteresEmCache> cache = new ConcurrentHashMap<>();

    private record PosteresEmCache(PostersDTO posteres, Instant em) {}

    /** o pôster do filme primeiro; depois os em português, os em inglês e os sem texto, dos mais votados aos outros */
    public PostersDTO doFilme(Long movieId) {
        return emCache("filme-" + movieId, () -> {
            Movie filme = movieRepository.findById(movieId)
                    .orElseThrow(() -> new ResourceNotFoundException("Filme não encontrado. Id: " + movieId));
            return montar(movieId, filme.getPosterPath(), () -> {
                if (filme.getTmdbId() == null) return null;
                TmdbMovieDetailDTO detalhe = tmdbClient.buscarDetalhesComImagens(filme.getTmdbId());
                return detalhe.getImages();
            });
        });
    }

    /** como em `doFilme`, com o pôster da série primeiro */
    public PostersDTO daSerie(Long seriesId) {
        return emCache("serie-" + seriesId, () -> {
            Series serie = seriesRepository.findById(seriesId)
                    .orElseThrow(() -> new ResourceNotFoundException("Série não encontrada. Id: " + seriesId));
            return montar(seriesId, serie.getPosterPath(),
                    () -> serie.getTmdbId() == null ? null : tmdbClient.buscarImagensSerie(serie.getTmdbId()));
        });
    }

    private PostersDTO emCache(String chave, Supplier<PostersDTO> buscar) {
        PosteresEmCache emCache = cache.get(chave);
        if (emCache != null && emCache.em().plus(VALIDADE).isAfter(Instant.now())) {
            return emCache.posteres();
        }
        PostersDTO resultado = buscar.get();
        CacheLimitado.guardar(cache, chave, new PosteresEmCache(resultado, Instant.now()));
        return resultado;
    }

    private static PostersDTO montar(Long id, String padrao, Supplier<TmdbMovieImagesDTO> imagensDoTmdb) {
        List<TmdbMovieImagesDTO.Image> imagens = List.of();
        try {
            TmdbMovieImagesDTO resposta = imagensDoTmdb.get();
            if (resposta != null) imagens = resposta.getPosters();
        } catch (HttpClientErrorException.NotFound e) {
            // sumiu do TMDB: fica só o pôster que já está salvo
        }

        List<String> caminhos = imagens.stream()
                .filter(i -> i.getFilePath() != null)
                .sorted(Comparator
                        .comparing((TmdbMovieImagesDTO.Image i) -> ordemDoIdioma(i.getLanguage()))
                        .thenComparing(i -> i.getVoteAverage() == null ? 0 : i.getVoteAverage(), Comparator.reverseOrder()))
                .map(TmdbMovieImagesDTO.Image::getFilePath)
                .toList();
        if (padrao != null) caminhos = Stream.concat(Stream.of(padrao), caminhos.stream()).toList();
        return new PostersDTO(id, padrao, caminhos.stream().distinct().limit(MAX_POSTERES).toList());
    }

    private static int ordemDoIdioma(String idioma) {
        if ("pt".equals(idioma)) return 0;
        if ("en".equals(idioma)) return 1;
        return idioma == null ? 2 : 3;
    }

    /**
     * O caminho a guardar na review: nulo para o pôster do filme. Só aceita um pôster que é mesmo
     * deste filme: o caminho nunca vem livre do cliente.
     */
    public String conferirFilme(Movie filme, String caminho) {
        if (caminho == null || caminho.isBlank() || caminho.equals(filme.getPosterPath())) return null;
        return conferir(doFilme(filme.getId()), caminho, "Esse pôster não é deste filme.");
    }

    /** como em `conferirFilme`: nulo para o pôster da série */
    public String conferirSerie(Series serie, String caminho) {
        if (caminho == null || caminho.isBlank() || caminho.equals(serie.getPosterPath())) return null;
        return conferir(daSerie(serie.getId()), caminho, "Esse pôster não é desta série.");
    }

    private static String conferir(PostersDTO posteres, String caminho, String erro) {
        if (!posteres.posters().contains(caminho)) throw new IllegalArgumentException(erro);
        return caminho;
    }
}
