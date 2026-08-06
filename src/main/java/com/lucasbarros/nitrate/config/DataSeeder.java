package com.lucasbarros.nitrate.config;

import com.lucasbarros.nitrate.entities.*;
import com.lucasbarros.nitrate.repositories.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class DataSeeder implements CommandLineRunner {

    @Autowired
    private MovieRepository movieRepository;
    @Autowired
    private DirectorRepository directorRepository;
    @Autowired
    private ActorRepository actorRepository;
    @Autowired
    private GenreRepository genreRepository;
    @Autowired
    private FranchiseRepository franchiseRepository;

    private Map<String, Director> diretoresCriados = new HashMap<>();
    private Map<String, Actor> atoresCriados = new HashMap<>();
    private Map<String, Genre> generosCriados = new HashMap<>();
    private Map<String, Franchise> franquiasCriadas = new HashMap<>();

    @Override
    public void run(String... args) {
        if (movieRepository.count() > 0) {
            System.out.println("Banco já populado, seeder não vai rodar de novo.");
            return;
        }

        System.out.println("Populando o banco com o catálogo mockado...");
        cadastrarFilmes();
        System.out.println("Catálogo cadastrado com sucesso!");
    }

    private void cadastrarFilmes() {
        Director nolan = obterOuCriarDiretor("Christopher Nolan");
        Director villeneuve = obterOuCriarDiretor("Denis Villeneuve");
        Director favreau = obterOuCriarDiretor("Jon Favreau");
        Director whedon = obterOuCriarDiretor("Joss Whedon");
        Director cameron = obterOuCriarDiretor("James Cameron");
        Director bongJoonHo = obterOuCriarDiretor("Bong Joon-ho");

        Franchise darkKnightTrilogy = obterOuCriarFranquia("The Dark Knight Trilogy");
        Franchise dune = obterOuCriarFranquia("Dune");
        Franchise mcu = obterOuCriarFranquia("Marvel Cinematic Universe");
        Franchise avatar = obterOuCriarFranquia("Avatar");

        Movie inception = criarFilme("Inception", 2010, 8.8, nolan, null);
        adicionarGeneros(inception, "Ficção Científica", "Ação");
        adicionarElenco(inception,
                ator("Leonardo DiCaprio", RoleType.PROTAGONIST),
                ator("Tom Hardy", RoleType.SUPPORTING),
                ator("Michael Caine", RoleType.CAMEO));

        Movie darkKnight = criarFilme("The Dark Knight", 2008, 9.0, nolan, darkKnightTrilogy);
        adicionarGeneros(darkKnight, "Ação", "Drama");
        adicionarElenco(darkKnight,
                ator("Christian Bale", RoleType.PROTAGONIST),
                ator("Heath Ledger", RoleType.PROTAGONIST),
                ator("Michael Caine", RoleType.SUPPORTING));

        Movie interstellar = criarFilme("Interstellar", 2014, 8.6, nolan, null);
        adicionarGeneros(interstellar, "Ficção Científica", "Drama");
        adicionarElenco(interstellar,
                ator("Matthew McConaughey", RoleType.PROTAGONIST),
                ator("Anne Hathaway", RoleType.SUPPORTING),
                ator("Michael Caine", RoleType.SUPPORTING));

        Movie oppenheimer = criarFilme("Oppenheimer", 2023, 8.9, nolan, null);
        adicionarGeneros(oppenheimer, "Drama", "História");
        adicionarElenco(oppenheimer,
                ator("Cillian Murphy", RoleType.PROTAGONIST),
                ator("Robert Downey Jr.", RoleType.SUPPORTING));

        Movie duneUm = criarFilme("Dune", 2021, 8.0, villeneuve, dune);
        adicionarGeneros(duneUm, "Ficção Científica", "Aventura");
        adicionarElenco(duneUm,
                ator("Timothée Chalamet", RoleType.PROTAGONIST),
                ator("Zendaya", RoleType.SUPPORTING),
                ator("Josh Brolin", RoleType.SUPPORTING));

        Movie duneDois = criarFilme("Dune: Part Two", 2024, 8.7, villeneuve, dune);
        adicionarGeneros(duneDois, "Ficção Científica", "Aventura");
        adicionarElenco(duneDois,
                ator("Timothée Chalamet", RoleType.PROTAGONIST),
                ator("Zendaya", RoleType.PROTAGONIST),
                ator("Josh Brolin", RoleType.SUPPORTING));

        Movie bladeRunner = criarFilme("Blade Runner 2049", 2017, 8.0, villeneuve, null);
        adicionarGeneros(bladeRunner, "Ficção Científica", "Drama");
        adicionarElenco(bladeRunner,
                ator("Ryan Gosling", RoleType.PROTAGONIST),
                ator("Harrison Ford", RoleType.SUPPORTING));

        Movie ironMan = criarFilme("Iron Man", 2008, 7.9, favreau, mcu);
        adicionarGeneros(ironMan, "Ação", "Ficção Científica");
        adicionarElenco(ironMan, ator("Robert Downey Jr.", RoleType.PROTAGONIST));

        Movie avengers = criarFilme("The Avengers", 2012, 8.0, whedon, mcu);
        adicionarGeneros(avengers, "Ação", "Ficção Científica");
        adicionarElenco(avengers,
                ator("Robert Downey Jr.", RoleType.PROTAGONIST),
                ator("Chris Evans", RoleType.PROTAGONIST));

        Movie titanic = criarFilme("Titanic", 1997, 7.9, cameron, null);
        adicionarGeneros(titanic, "Drama", "Romance");
        adicionarElenco(titanic,
                ator("Leonardo DiCaprio", RoleType.PROTAGONIST),
                ator("Kate Winslet", RoleType.PROTAGONIST));

        Movie avatarFilme = criarFilme("Avatar", 2009, 7.9, cameron, avatar);
        adicionarGeneros(avatarFilme, "Ficção Científica", "Aventura");
        adicionarElenco(avatarFilme,
                ator("Sam Worthington", RoleType.PROTAGONIST),
                ator("Zoe Saldana", RoleType.PROTAGONIST));

        Movie parasite = criarFilme("Parasite", 2019, 8.5, bongJoonHo, null);
        adicionarGeneros(parasite, "Drama", "Suspense");
        adicionarElenco(parasite, ator("Song Kang-ho", RoleType.PROTAGONIST));
    }

    private Director obterOuCriarDiretor(String nome) {
        if (diretoresCriados.containsKey(nome)) {
            return diretoresCriados.get(nome);
        }
        Director diretor = directorRepository.save(new Director(nome));
        diretoresCriados.put(nome, diretor);
        return diretor;
    }

    private Actor obterOuCriarAtor(String nome) {
        if (atoresCriados.containsKey(nome)) {
            return atoresCriados.get(nome);
        }
        Actor atorCriado = actorRepository.save(new Actor(nome));
        atoresCriados.put(nome, atorCriado);
        return atorCriado;
    }

    private Genre obterOuCriarGenero(String nome) {
        if (generosCriados.containsKey(nome)) {
            return generosCriados.get(nome);
        }
        Genre genero = genreRepository.save(new Genre(nome));
        generosCriados.put(nome, genero);
        return genero;
    }

    private Franchise obterOuCriarFranquia(String nome) {
        if (franquiasCriadas.containsKey(nome)) {
            return franquiasCriadas.get(nome);
        }
        Franchise franquia = franchiseRepository.save(new Franchise(nome));
        franquiasCriadas.put(nome, franquia);
        return franquia;
    }

    private Movie criarFilme(String titulo, int ano, double nota, Director diretor, Franchise franquia) {
        Movie filme = new Movie(titulo, ano, nota, diretor, franquia);
        return movieRepository.save(filme);
    }

    private void adicionarGeneros(Movie filme, String... nomesDosGeneros) {
        for (String nome : nomesDosGeneros) {
            filme.getGenres().add(obterOuCriarGenero(nome));
        }
        movieRepository.save(filme);
    }

    private static class ElencoInfo {
        String nomeDoAtor;
        RoleType papel;

        ElencoInfo(String nomeDoAtor, RoleType papel) {
            this.nomeDoAtor = nomeDoAtor;
            this.papel = papel;
        }
    }

    private ElencoInfo ator(String nome, RoleType papel) {
        return new ElencoInfo(nome, papel);
    }

    private void adicionarElenco(Movie filme, ElencoInfo... elenco) {
        for (ElencoInfo info : elenco) {
            Actor atorEntidade = obterOuCriarAtor(info.nomeDoAtor);
            MovieCast movieCast = new MovieCast(filme, atorEntidade, info.papel);
            filme.getCast().add(movieCast);
        }
        movieRepository.save(filme);
    }
}