package com.lucasbarros.nitrate.recommender;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.graph.ConnectionType;
import com.lucasbarros.nitrate.graph.Edge;
import com.lucasbarros.nitrate.graph.MovieGraph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class RecommendationEngine {
    private static final double DIMINISHING_FACTOR = 0.4;
    // nota do TMDB só desempata de leve: um 9 vale 15% a mais, um 4 vale 15% a menos
    private static final double NOTA_BAIXA = 4.0;
    private static final double NOTA_ALTA = 9.0;
    private static final double AJUSTE_MAXIMO_NOTA = 0.15;

    // o que se parece com filmes que a pessoa rejeitou perde pontos: cada ponto de semelhança com eles
    // divide a pontuação por (1 + 0,5 × ponto)
    private static final double PESO_NEGATIVO = 0.5;
    // gênero é amplo demais para pesar contra: não gostar de um drama não é não gostar de drama
    private static final Set<ConnectionType> PESAM_CONTRA = EnumSet.of(ConnectionType.DIRECTOR, ConnectionType.ACTOR,
            ConnectionType.FRANCHISE, ConnectionType.CO_LIKED);

    // maior pontuação primeiro; empate fica com o menor id, para a ordem não depender do HashMap
    private static final Comparator<Candidato> MELHOR_PRIMEIRO = Comparator
            .comparingDouble((Candidato c) -> c.score).reversed()
            .thenComparing(c -> c.movie.getId(), Comparator.nullsLast(Comparator.naturalOrder()));

    /** cada parte pode ser desligada, para a medição comparar com e sem ela */
    public record Opcoes(boolean pesoDasSementes, boolean sinaisNegativos, boolean notaDoTmdb) {
        public static final Opcoes PADRAO = new Opcoes(true, true, true);
    }

    private MovieGraph graph;
    private Opcoes opcoes;

    public RecommendationEngine(MovieGraph graph) {
        this(graph, Opcoes.PADRAO);
    }

    public RecommendationEngine(MovieGraph graph, Opcoes opcoes) {
        this.graph = graph;
        this.opcoes = opcoes;
    }

    public List<RecommendationResult> recommend(UserPreferences preferences, int topN) {
        if (topN <= 0) return new ArrayList<>();
        Set<Movie> foraDoResultado = new HashSet<>(preferences.getLikedMovies());
        foraDoResultado.addAll(preferences.getDislikedMovies());
        Map<Movie, Candidato> candidatos = new HashMap<>();

        for (Movie liked : preferences.getLikedMovies()) {
            double pesoDaSemente = opcoes.pesoDasSementes() ? preferences.getSeedWeight(liked) : 1.0;
            for (Edge edge : graph.getConnections(liked, preferences.getIgnoredUserId())) {
                Movie candidate = edge.getTarget();
                if (foraDoResultado.contains(candidate)) continue;

                double multiplicador = preferences.getConnectionTypeMultiplier(edge.getType());
                if (multiplicador <= 0) continue; // tipo desligado (medição)

                double valor = edge.getWeight() * multiplicador * calcularBoostFavorito(edge, preferences) * pesoDaSemente;
                candidatos.computeIfAbsent(candidate, Candidato::new).contributions.add(new Contribution(edge, liked, valor));
            }
        }

        if (opcoes.sinaisNegativos()) {
            aplicarRejeitados(preferences, candidatos);
        }

        // só a pontuação para todos; os motivos (strings, mapas) só para quem entra no resultado
        PriorityQueue<Candidato> melhores = new PriorityQueue<>(Math.min(topN, Math.max(1, candidatos.size())) + 1,
                MELHOR_PRIMEIRO.reversed());
        for (Candidato candidato : candidatos.values()) {
            candidato.pontuar(opcoes);
            melhores.add(candidato);
            if (melhores.size() > topN) melhores.poll();
        }

        List<Candidato> escolhidos = new ArrayList<>(melhores);
        escolhidos.sort(MELHOR_PRIMEIRO);
        List<RecommendationResult> results = new ArrayList<>(escolhidos.size());
        for (Candidato candidato : escolhidos) {
            results.add(candidato.gerarResultado());
        }
        return results;
    }

    /** soma, para cada candidato, o quanto ele se parece com os filmes rejeitados */
    private void aplicarRejeitados(UserPreferences preferences, Map<Movie, Candidato> candidatos) {
        for (Movie rejeitado : preferences.getDislikedMovies()) {
            for (Edge edge : graph.getConnections(rejeitado, preferences.getIgnoredUserId())) {
                if (!PESAM_CONTRA.contains(edge.getType())) continue;
                Candidato candidato = candidatos.get(edge.getTarget());
                if (candidato == null) continue;
                candidato.semelhancaComRejeitados += edge.getWeight() * preferences.getConnectionTypeMultiplier(edge.getType());
            }
        }
    }

    private double calcularBoostFavorito(Edge edge, UserPreferences preferences) {
        if (edge.getType() == ConnectionType.DIRECTOR && preferences.isFavoriteDirector(edge.getAttribute())) {
            return preferences.getFavoriteBoost();
        }
        if (edge.getType() == ConnectionType.ACTOR && preferences.isFavoriteActor(edge.getAttribute())) {
            return preferences.getFavoriteBoost();
        }
        return 1.0;
    }

    /** sem nota (ou 0, filme que ainda não estreou) fica neutro */
    private static double fatorDeNota(Movie movie) {
        Double nota = movie.getRating();
        if (nota == null || nota <= 0) return 1.0;
        double limitada = Math.max(NOTA_BAIXA, Math.min(NOTA_ALTA, nota));
        double posicao = (limitada - NOTA_BAIXA) / (NOTA_ALTA - NOTA_BAIXA); // 0..1
        return 1.0 + AJUSTE_MAXIMO_NOTA * (2 * posicao - 1);
    }

    private static class Candidato {
        final Movie movie;
        // na ordem dos filmes curtidos (mais recente primeiro), que é a ordem citada nos motivos
        final List<Contribution> contributions = new ArrayList<>(4);
        double semelhancaComRejeitados;
        double score;

        Candidato(Movie movie) {
            this.movie = movie;
        }

        /**
         * Cada conexão repetida do mesmo tipo vale menos que a anterior, e a mais forte de cada tipo
         * é a que conta cheia: assim a pontuação não depende da ordem em que as arestas chegaram.
         */
        void pontuar(Opcoes opcoes) {
            List<Contribution> porForca = contributions;
            if (contributions.size() > 1) {
                porForca = new ArrayList<>(contributions);
                porForca.sort(Comparator.comparing((Contribution c) -> c.edge.getType())
                        .thenComparing(Comparator.comparingDouble((Contribution c) -> c.valor).reversed()));
            }

            double total = 0;
            ConnectionType tipoAtual = null;
            int repetidas = 0;
            for (Contribution c : porForca) {
                if (c.edge.getType() != tipoAtual) {
                    tipoAtual = c.edge.getType();
                    repetidas = 0;
                }
                c.efetiva = c.valor / (1.0 + repetidas * DIMINISHING_FACTOR);
                total += c.efetiva;
                repetidas++;
            }
            if (opcoes.notaDoTmdb()) total *= fatorDeNota(movie);
            score = total / (1.0 + PESO_NEGATIVO * semelhancaComRejeitados);
        }

        RecommendationResult gerarResultado() {
            Map<String, Double> somaPorMotivo = new HashMap<>();
            // os filmes curtidos que geraram cada motivo, na ordem em que foram curtidos (mais recente primeiro)
            Map<String, LinkedHashSet<String>> origensPorMotivo = new HashMap<>();

            for (Contribution c : contributions) {
                String motivo = c.edge.getReason();
                somaPorMotivo.merge(motivo, c.efetiva, Double::sum);
                origensPorMotivo.computeIfAbsent(motivo, k -> new LinkedHashSet<>()).add(tituloCurto(c.origem.getTitle()));
            }

            List<String> motivosOrdenados = new ArrayList<>(somaPorMotivo.keySet());
            motivosOrdenados.sort(Comparator.comparingDouble((String m) -> somaPorMotivo.get(m)).reversed());

            List<String> reasons = new ArrayList<>();
            int limite = Math.min(4, motivosOrdenados.size());
            for (int i = 0; i < limite; i++) {
                String motivo = motivosOrdenados.get(i);
                String origens = listarOrigens(new ArrayList<>(origensPorMotivo.get(motivo)));
                // "quem curtiu {} também curtiu" leva os filmes no meio; os outros, "..., como em X"
                reasons.add(motivo.contains("{}") ? motivo.replace("{}", origens) : motivo + ", como em " + origens);
            }

            double arredondado = Math.round(score * 100.0) / 100.0;
            return new RecommendationResult(movie, arredondado, reasons);
        }

        /** "La La Land: Cantando Estações" vira "La La Land": o motivo cita vários títulos numa linha */
        static String tituloCurto(String titulo) {
            int corte = titulo.indexOf(": ");
            return corte >= 3 ? titulo.substring(0, corte) : titulo;
        }

        /** "Oppenheimer", "Oppenheimer e Tenet", "Oppenheimer, Tenet e mais 2" */
        private String listarOrigens(List<String> titulos) {
            if (titulos.size() == 1) return titulos.get(0);
            if (titulos.size() == 2) return titulos.get(0) + " e " + titulos.get(1);
            int resto = titulos.size() - 2;
            return titulos.get(0) + ", " + titulos.get(1) + " e mais " + resto;
        }
    }

    private static class Contribution {
        final Edge edge;
        final Movie origem;
        final double valor;
        double efetiva;

        Contribution(Edge edge, Movie origem, double valor) {
            this.edge = edge;
            this.origem = origem;
            this.valor = valor;
        }
    }
}
