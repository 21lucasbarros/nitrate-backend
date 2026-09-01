package com.lucasbarros.nitrate.tmdb;

import com.lucasbarros.nitrate.entities.*;
import com.lucasbarros.nitrate.repositories.*;
import com.lucasbarros.nitrate.tmdb.dto.*;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

@Service
public class TmdbImportService {

    @Autowired private TmdbClient tmdbClient;
    @Autowired private MovieRepository movieRepository;
    @Autowired private DirectorRepository directorRepository;
    @Autowired private ActorRepository actorRepository;
    @Autowired private GenreRepository genreRepository;
    @Autowired private FranchiseRepository franchiseRepository;

    private Map<String, Director> diretoresCache = new Hashtable<>();
    private Map<String, Actor> atoresCache = new HashMap<>();
    private Map<String, Genre> generosCache = new HashMap<>();
    private Map<String, Franchise> franquiasCache = new HashMap<>();

    private static final int MAX_ATORES_POR_FILME = 8;

    @Transactional
    public int importarFilmesPopulares(int paginas) {
        int totalImportado = 0;

        for (int pagina = 1; pagina <= paginas; pagina++) {
            TmdbPopularResponseDTO popular = tmdbClient.buscarFilmesPopulares(pagina);

            for (TmdbMovieSummaryDTO resumo : popular.getResults()) {
                if (movieRepository.existsByTmdbId(resumo.getId())) {
                    continue;
                }
                importarFilme(resumo.getId());
                totalImportado++;
            }
        }
        return totalImportado;
    }

    private void importarFilme(Long tmdbId) {
        TmdbMovieDetailDTO detalhe = tmdbClient.buscarDetalhesComCreditos(tmdbId);

        Director diretor = extrairDiretor(detalhe);
        Franchise franquia = extrairFranquia(detalhe);

        Movie filme = new Movie();
        filme.setTmdbId(detalhe.getId());
        filme.setTitle(detalhe.getTitle());
        filme.setOverview(detalhe.getOverview());
        filme.setPosterPath(detalhe.getPosterPath());
        filme.setRating(detalhe.getVoteAverage());
        filme.setReleaseYear(extrairAno(detalhe.getReleaseDate()));
        filme.setDirector(diretor);
        filme.setFranchise(franquia);

        for (TmdbGenreDTO generoTmdb : detalhe.getGenres()) {
            filme.getGenres().add(obterOuCriarGenero(generoTmdb.getName()));
        }

        filme = movieRepository.save(filme);
        adicionarElenco(filme, detalhe);
        movieRepository.save(filme);
    }

    private Director extrairDiretor(TmdbMovieDetailDTO detalhe) {
        if (detalhe.getCredits() == null) return null;

        for (TmdbCrewMemberDTO membroDaEquipe : detalhe.getCredits().getCrew()) {
            if ("Director".equals(membroDaEquipe.getJob())) {
                return obterOuCriarDiretor(membroDaEquipe.getName());
            }
        }
        return null;
    }

    private Franchise extrairFranquia(TmdbMovieDetailDTO detalhe) {
        if (detalhe.getBelongsToCollection() == null) return null;
        return obterOuCriarFranquia(detalhe.getBelongsToCollection().getName());
    }

    private Integer extrairAno(String releaseDate) {
        if (releaseDate == null || releaseDate.length() < 4) return null;
        return Integer.parseInt(releaseDate.substring(0, 4));
    }

    private void adicionarElenco(Movie filme, TmdbMovieDetailDTO detalhe) {
        if (detalhe.getCredits() == null) return;

        int quantidade = Math.min(MAX_ATORES_POR_FILME, detalhe.getCredits().getCast().size());

        for (int i = 0; i < quantidade; i++) {
            TmdbCastMemberDTO membro = detalhe.getCredits().getCast().get(i);
            RoleType papel = definirPapel(membro.getOrder());

            Actor ator = obterOuCriarAtor(membro.getName());
            filme.getCast().add(new MovieCast(filme, ator, papel));
        }
    }

    private RoleType definirPapel(Integer ordem) {
        if (ordem == null) return RoleType.CAMEO;
        if (ordem <= 2) return RoleType.PROTAGONIST;
        if (ordem <= 6) return RoleType.SUPPORTING;
        return RoleType.CAMEO;
    }

    private Director obterOuCriarDiretor(String nome) {
        if (diretoresCache.containsKey(nome)) return diretoresCache.get(nome);
        Director d = directorRepository.findByName(nome).orElseGet(() -> directorRepository.save(new Director(nome)));
        diretoresCache.put(nome, d);
        return d;
    }

    private Actor obterOuCriarAtor(String nome) {
        if (atoresCache.containsKey(nome)) return atoresCache.get(nome);
        Actor a = actorRepository.findByName(nome).orElseGet(() -> actorRepository.save(new Actor(nome)));
        atoresCache.put(nome, a);
        return a;
    }

    private Genre obterOuCriarGenero(String nome) {
        if (generosCache.containsKey(nome)) return generosCache.get(nome);
        Genre g = genreRepository.findByName(nome).orElseGet(() -> genreRepository.save(new Genre(nome)));
        generosCache.put(nome, g);
        return g;
    }

    private Franchise obterOuCriarFranquia(String nome) {
        if (franquiasCache.containsKey(nome)) return franquiasCache.get(nome);
        Franchise f = franchiseRepository.findByName(nome).orElseGet(() -> franchiseRepository.save(new Franchise(nome)));
        franquiasCache.put(nome, f);
        return f;
    }
}
