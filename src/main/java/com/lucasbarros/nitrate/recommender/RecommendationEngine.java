package com.lucasbarros.nitrate.recommender;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.graph.ConnectionType;
import com.lucasbarros.nitrate.graph.Edge;
import com.lucasbarros.nitrate.graph.MovieGraph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecommendationEngine {
    private static final double DIMINISHING_FACTOR = 0.4;

    private MovieGraph graph;

    public RecommendationEngine(MovieGraph graph) {
        this.graph = graph;
    }

    public List<RecommendationResult> recommend(UserPreferences preferences, int topN) {
        Map<Movie, ScoreAccumulator> accumulators = new HashMap<>();

        for (Movie liked : preferences.getLikedMovies()) {
            List<Edge> edges = graph.getConnections(liked);

            for (Edge edge : edges) {
                Movie candidate = edge.getTarget();

                if (preferences.getLikedMovies().contains(candidate)) {
                    continue;
                }

                ScoreAccumulator accumulator = accumulators.get(candidate);
                if (accumulator == null) {
                    accumulator = new ScoreAccumulator();
                    accumulators.put(candidate, accumulator);
                }

                accumulator.aplicarConexao(edge, preferences);
            }
        }

        List<RecommendationResult> results = new ArrayList<>();
        for (Map.Entry<Movie, ScoreAccumulator> entry : accumulators.entrySet()) {
            results.add(entry.getValue().gerarResultado(entry.getKey()));
        }

        ordenarPorPontuacaoDecrescente(results);

        if (results.size() > topN) {
            return results.subList(0, topN);
        }
        return results;
    }

    private void ordenarPorPontuacaoDecrescente(List<RecommendationResult> results) {
        for (int i = 0; i < results.size() - 1; i++) {
            int indiceDoMaior = i;
            for (int j = i + 1; j < results.size(); j++) {
                if (results.get(j).getScore() > results.get(indiceDoMaior).getScore()) {
                    indiceDoMaior = j;
                }
            }
            if (indiceDoMaior != i) {
                RecommendationResult temp = results.get(i);
                results.set(i, results.get(indiceDoMaior));
                results.set(indiceDoMaior, temp);
            }
        }
    }

    private static class ScoreAccumulator {
        private double totalScore = 0;
        private Map<ConnectionType, Integer> typeCounts = new HashMap<>();
        private List<ReasonContribution> contributions = new ArrayList<>();

        void aplicarConexao(Edge edge, UserPreferences preferences) {
            int countSoFar = 0;
            if (typeCounts.containsKey(edge.getType())) {
                countSoFar = typeCounts.get(edge.getType());
            }

            double diminishing = 1.0 / (1.0 + countSoFar * DIMINISHING_FACTOR);
            double typeMultiplier = preferences.getConnectionTypeMultiplier(edge.getType());
            double favoriteMultiplier = calcularBoostFavorito(edge, preferences);

            double contribution = edge.getWeight() * diminishing * typeMultiplier * favoriteMultiplier;

            totalScore = totalScore + contribution;
            typeCounts.put(edge.getType(), countSoFar + 1);
            contributions.add(new ReasonContribution(edge.getReason(), contribution));
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

        RecommendationResult gerarResultado(Movie movie) {
            Map<String, Double> somaPorMotivo = new HashMap<>();
            Map<String, Integer> vezesPorMotivo = new HashMap<>();

            for (ReasonContribution rc : contributions) {
                double somaAtual = 0;
                int vezesAtual = 0;
                if (somaPorMotivo.containsKey(rc.reason)) {
                    somaAtual = somaPorMotivo.get(rc.reason);
                    vezesAtual = vezesPorMotivo.get(rc.reason);
                }
                somaPorMotivo.put(rc.reason, somaAtual + rc.contribution);
                vezesPorMotivo.put(rc.reason, vezesAtual + 1);
            }

            List<String> motivosOrdenados = ordenarMotivosPorForca(somaPorMotivo);

            List<String> reasons = new ArrayList<>();
            int limite = Math.min(4, motivosOrdenados.size());
            for (int i = 0; i < limite; i++) {
                String motivo = motivosOrdenados.get(i);
                int vezes = vezesPorMotivo.get(motivo);
                if (vezes > 1) {
                    reasons.add(motivo + " (reforçado por " + vezes + " filmes curtidos)");
                } else {
                    reasons.add(motivo);
                }
            }

            double arredondado = Math.round(totalScore * 100.0) / 100.0;
            return new RecommendationResult(movie, arredondado, reasons);
        }

        private List<String> ordenarMotivosPorForca(Map<String, Double> somaPorMotivo) {
            List<String> motivos = new ArrayList<>(somaPorMotivo.keySet());

            for (int i = 0; i < motivos.size() - 1; i++) {
                int indiceDoMaior = i;
                for (int j = i + 1; j < motivos.size(); j++) {
                    double forcaJ = somaPorMotivo.get(motivos.get(j));
                    double forcaAtualMaior = somaPorMotivo.get(motivos.get(indiceDoMaior));
                    if (forcaJ > forcaAtualMaior) {
                        indiceDoMaior = j;
                    }
                }
                if (indiceDoMaior != i) {
                    String temp = motivos.get(i);
                    motivos.set(i, motivos.get(indiceDoMaior));
                    motivos.set(indiceDoMaior, temp);
                }
            }
            return motivos;
        }
    }

    private static class ReasonContribution {
        String reason;
        double contribution;

        ReasonContribution(String reason, double contribution) {
            this.reason = reason;
            this.contribution = contribution;
        }
    }
}
