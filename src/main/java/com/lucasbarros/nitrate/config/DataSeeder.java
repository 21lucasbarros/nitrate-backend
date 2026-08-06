package com.lucasbarros.nitrate.config;

import com.lucasbarros.nitrate.entities.Actor;
import com.lucasbarros.nitrate.entities.Director;
import com.lucasbarros.nitrate.entities.Franchise;
import com.lucasbarros.nitrate.entities.Genre;
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
    private Map<String, Franchise> franquiaCriados = new HashMap<>();

    @Override
    public void run(String... args) {
        if(movieRepository.count() > 0) {
            System.out.println("Banco já populado, seeder não vai rodar de novo.");
            return;
        }

        System.out.println("Populando o banco com o catálogo mockado...");
    }

    private void cadastrarFilmes() {
    }

    private Director obterOuCriarDiretor(String nome) {
        if(diretoresCriados.containsKey(nome)) {
            return diretoresCriados.get(nome);
        }
    }
}
