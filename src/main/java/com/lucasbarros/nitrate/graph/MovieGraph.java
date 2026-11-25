package com.lucasbarros.nitrate.graph;

import com.lucasbarros.nitrate.entities.Genre;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.MovieCast;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MovieGraph {
    // gênero presente em até 5% do catálogo conta como raro (peso cheio)
    private static final double FRACAO_GENERO_RARO = 0.05;
    private static final double PESO_MINIMO_GENERO = 0.2;

    // "quem curtiu também curtiu": uma pessoa sozinha não basta, é coincidência
    private static final int MINIMO_CURTIDAS_EM_COMUM = 2;
    // encolhe a semelhança quando há poucas pessoas em comum: 2 em comum vale 50%, 6 vale 75%
    private static final double ENCOLHIMENTO_CURTIDAS = 2.0;
    // o cosseno de dois filmes de fato parecidos costuma ficar entre 0,1 e 0,3: multiplicado por 3,
    // vira peso comparável ao de um ator em comum
    private static final double ESCALA_COSSENO = 3.0;

    private List<Movie> movies;
    private Map<Long, Movie> porId = new HashMap<>();

    // as chaves são o id da pessoa/gênero/franquia (nomes iguais não se misturam); sem id, o nome
    private Map<String, List<Movie>> byDirection = new HashMap<>();
    // o papel de cada filme vem junto: não precisa procurar o ator no elenco do candidato
    private Map<String, List<MovieCast>> byActor = new HashMap<>();
    private Map<String, List<Movie>> byGenre = new HashMap<>();
    private Map<String, List<Movie>> byFranchise = new HashMap<>();

    // curtidas: quem curtiu cada filme (índices de pessoa) e o que cada pessoa curtiu
    private Map<Long, Integer> indicePorUsuario = new HashMap<>();
    private List<Movie[]> curtidasPorUsuario = new ArrayList<>();
    private Map<Long, int[]> quemCurtiu = new HashMap<>();

    public MovieGraph(List<Movie> movies) {
        this(movies, Map.of());
    }

    /** @param curtidas para cada pessoa, os ids dos filmes que ela curtiu ou avaliou bem */
    public MovieGraph(List<Movie> movies, Map<Long, ? extends Collection<Long>> curtidas) {
        this.movies = movies;
        montarIndice();
        montarCurtidas(curtidas);
    }

    private static <T> void adicionarNoIndice(Map<String, List<T>> mapa, String chave, T valor) {
        mapa.computeIfAbsent(chave, k -> new ArrayList<>()).add(valor);
    }

    private static String chave(Long id, String nome) {
        return id != null ? "#" + id : nome.toLowerCase();
    }

    private void montarIndice() {
        for(Movie movie : movies) {
            if(movie.getId() != null) {
                porId.put(movie.getId(), movie);
            }

            if(movie.getDirector() != null) {
                adicionarNoIndice(byDirection, chave(movie.getDirector().getId(), movie.getDirector().getName()), movie);
            }

            for(MovieCast membro : movie.getCast()) {
                adicionarNoIndice(byActor, chave(membro.getActor().getId(), membro.getActor().getName()), membro);
            }

            for(Genre genero : movie.getGenres()) {
                adicionarNoIndice(byGenre, chave(genero.getId(), genero.getName()), movie);
            }

            if(movie.getFranchise() != null) {
                adicionarNoIndice(byFranchise, chave(movie.getFranchise().getId(), movie.getFranchise().getName()), movie);
            }
        }
    }

    private void montarCurtidas(Map<Long, ? extends Collection<Long>> curtidas) {
        Map<Long, List<Integer>> quem = new HashMap<>();
        for (Map.Entry<Long, ? extends Collection<Long>> pessoa : curtidas.entrySet()) {
            List<Movie> filmes = new ArrayList<>();
            for (Long movieId : new HashSet<>(pessoa.getValue())) {
                Movie filme = porId.get(movieId);
                if (filme != null) filmes.add(filme);
            }
            if (filmes.isEmpty()) continue;
            int indice = curtidasPorUsuario.size();
            indicePorUsuario.put(pessoa.getKey(), indice);
            curtidasPorUsuario.add(filmes.toArray(new Movie[0]));
            for (Movie filme : filmes) {
                quem.computeIfAbsent(filme.getId(), k -> new ArrayList<>()).add(indice);
            }
        }
        for (Map.Entry<Long, List<Integer>> filme : quem.entrySet()) {
            quemCurtiu.put(filme.getKey(), filme.getValue().stream().mapToInt(Integer::intValue).toArray());
        }
    }

    public List<Edge> getConnections(Movie source) {
        return getConnections(source, null);
    }

    /**
     * @param ignorarUsuario curtidas dessa pessoa ficam de fora do "quem curtiu também curtiu"
     *                       (na medição, para o filme escondido não se denunciar pela própria curtida)
     */
    public List<Edge> getConnections(Movie source, Long ignorarUsuario) {
        List<Edge> edges = new ArrayList<>();
        conectarPorDiretor(source, edges);
        conectarPorAtores(source, edges);
        conectarPorGeneros(source, edges);
        conectarPorFranquia(source, edges);
        conectarPorCurtidas(source, ignorarUsuario, edges);
        return edges;
    }

    private void conectarPorDiretor(Movie source, List<Edge> edges) {
        if(source.getDirector() == null) return;

        String nomeDoDiretor = source.getDirector().getName();
        List<Movie> candidatos = byDirection.get(chave(source.getDirector().getId(), nomeDoDiretor));
        if(candidatos == null) return;

        String reason = "mesmo diretor (" + nomeDoDiretor + ")";
        for(Movie candidato : candidatos) {
            if(candidato.equals(source)) continue;
            edges.add(new Edge(candidato, ConnectionType.DIRECTOR, ConnectionType.DIRECTOR.getBaseWeight(), reason, nomeDoDiretor));
        }
    }

    private void conectarPorAtores(Movie source, List<Edge> edges) {
        for(MovieCast membro : source.getCast()) {
            String nomeDoAtor = membro.getActor().getName();
            List<MovieCast> participacoes = byActor.get(chave(membro.getActor().getId(), nomeDoAtor));
            if(participacoes == null) continue;

            String reason = "elenco em comum: " + nomeDoAtor;
            for(MovieCast outroMembro : participacoes) {
                Movie candidato = outroMembro.getMovie();
                if(candidato.equals(source)) continue;

                double roleFactor = Math.min(membro.getRole().getRelevance(), outroMembro.getRole().getRelevance());
                double weight = ConnectionType.ACTOR.getBaseWeight() * roleFactor;
                edges.add(new Edge(candidato, ConnectionType.ACTOR, weight, reason, nomeDoAtor));
            }
        }
    }

    private void conectarPorGeneros(Movie source, List<Edge> edges) {
        for (Genre genero : source.getGenres()) {
            List<Movie> candidatos = byGenre.get(chave(genero.getId(), genero.getName()));
            if (candidatos == null) continue;

            double weight = ConnectionType.GENRE.getBaseWeight() * raridadeDoGenero(candidatos.size());
            String reason = "mesmo gênero: " + genero.getName();
            for (Movie candidato : candidatos) {
                if (candidato.equals(source)) continue;
                edges.add(new Edge(candidato, ConnectionType.GENRE, weight, reason, genero.getName()));
            }
        }
    }

    /**
     * Gênero comum diz pouco: dividir "Drama" com metade do catálogo não aproxima dois filmes,
     * já dividir "Faroeste" sim. O peso cai com a fração do acervo que tem o gênero (estilo IDF):
     * até 5% do catálogo vale o peso cheio, metade do catálogo vale ~23%, e nunca menos de 20%.
     */
    private double raridadeDoGenero(int filmesNoGenero) {
        double fracao = (double) filmesNoGenero / Math.max(1, movies.size());
        double raridade = Math.log(1.0 / fracao) / Math.log(1.0 / FRACAO_GENERO_RARO);
        return Math.max(PESO_MINIMO_GENERO, Math.min(1.0, raridade));
    }

    private void conectarPorFranquia(Movie source, List<Edge> edges) {
        if (source.getFranchise() == null) return;

        String nomeDaFranquia = source.getFranchise().getName();
        List<Movie> candidatos = byFranchise.get(chave(source.getFranchise().getId(), nomeDaFranquia));
        if (candidatos == null) return;

        String reason = "mesma franquia: " + nomeDaFranquia;
        for (Movie candidato : candidatos) {
            if (candidato.equals(source)) continue;
            edges.add(new Edge(candidato, ConnectionType.FRANCHISE, ConnectionType.FRANCHISE.getBaseWeight(), reason, nomeDaFranquia));
        }
    }

    /**
     * Filmes que as mesmas pessoas curtiram. A semelhança é o cosseno entre os grupos de quem curtiu
     * (pessoas em comum / raiz do tamanho dos dois grupos): um filme que todo mundo curte aparece com
     * tudo, e a divisão pelo tamanho do grupo dele tira essa vantagem.
     */
    private void conectarPorCurtidas(Movie source, Long ignorarUsuario, List<Edge> edges) {
        int[] curtiramOrigem = quemCurtiu.get(source.getId());
        if (curtiramOrigem == null) return;

        Integer ignorado = ignorarUsuario == null ? null : indicePorUsuario.get(ignorarUsuario);
        Set<Movie> curtidosPeloIgnorado = new HashSet<>();
        if (ignorado != null) curtidosPeloIgnorado.addAll(List.of(curtidasPorUsuario.get(ignorado)));

        Map<Movie, Integer> emComum = new HashMap<>();
        int totalOrigem = 0;
        for (int pessoa : curtiramOrigem) {
            if (ignorado != null && pessoa == ignorado) continue;
            totalOrigem++;
            for (Movie outro : curtidasPorUsuario.get(pessoa)) {
                if (outro != source) emComum.merge(outro, 1, Integer::sum);
            }
        }

        String reason = "quem curtiu {} também curtiu";
        for (Map.Entry<Movie, Integer> par : emComum.entrySet()) {
            int comum = par.getValue();
            if (comum < MINIMO_CURTIDAS_EM_COMUM) continue;
            Movie candidato = par.getKey();
            int totalCandidato = quemCurtiu.get(candidato.getId()).length - (curtidosPeloIgnorado.contains(candidato) ? 1 : 0);
            double cosseno = comum / Math.sqrt((double) totalOrigem * totalCandidato);
            double confianca = comum / (comum + ENCOLHIMENTO_CURTIDAS);
            double weight = ConnectionType.CO_LIKED.getBaseWeight() * Math.min(1.0, cosseno * ESCALA_COSSENO) * confianca;
            edges.add(new Edge(candidato, ConnectionType.CO_LIKED, weight, reason, null));
        }
    }

    public List<Movie> getAllMovies() {
        return movies;
    }

    /** a instância do grafo para o id, ou null se o filme não está nele */
    public Movie getMovie(Long id) {
        return porId.get(id);
    }
}
