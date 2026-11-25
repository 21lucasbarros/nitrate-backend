package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.entities.FavoritePerson;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.TasteProfile;
import com.lucasbarros.nitrate.graph.ConnectionType;
import com.lucasbarros.nitrate.graph.MovieGraph;
import com.lucasbarros.nitrate.graph.MovieGraphCache;
import com.lucasbarros.nitrate.recommender.RecommendationEngine;
import com.lucasbarros.nitrate.recommender.RecommendationResult;
import com.lucasbarros.nitrate.repositories.FavoritePersonRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.repositories.TasteProfileRepository;
import com.lucasbarros.nitrate.services.RecommendationService.Ajustes;
import com.lucasbarros.nitrate.services.RecommendationService.Entrada;
import com.lucasbarros.nitrate.services.RecommendationService.Favorito;
import com.lucasbarros.nitrate.services.RecommendationService.Registro;
import com.lucasbarros.nitrate.taste.TasteMatcher.Preferencias;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Mede a recomendação com o próprio histórico do Nitrate: para cada pessoa, esconde os últimos
 * filmes que ela curtiu, recomenda com o diário de antes deles e confere quantos dos escondidos
 * aparecem entre os 12 recomendados. Cada parte pode ser desligada (`sem`), para ver quanto ela
 * ajuda: rode uma vez com tudo e outra sem a parte, e compare.
 */
@Service
public class RecommendationEvaluationService {

    private static final int TOP_N = 12;
    // precisa de diário suficiente para sobrar semente depois de esconder os últimos
    private static final int CURTIDAS_ALEM_DAS_ESCONDIDAS = 5;

    /** o que dá para desligar além dos tipos de conexão (ConnectionType) */
    public enum Parte { PESO_SEMENTES, NEGATIVOS, NOTA_TMDB }

    public record Resultado(int pessoasAvaliadas, int filmesEscondidosPorPessoa,
                            double acertoEm12, double recallEm12, double mrr,
                            double coberturaDoCatalogo, double mediaDeRecomendacoes,
                            List<String> desligado, long milissegundos) {}

    @Autowired private ReviewRepository reviewRepository;
    @Autowired private TasteProfileRepository tasteProfileRepository;
    @Autowired private FavoritePersonRepository favoritePersonRepository;
    @Autowired private RecommendationService recommendationService;
    @Autowired private MovieGraphCache graphCache;

    /**
     * @param desligar nomes de ConnectionType (CO_LIKED, GENRE, ...) ou de Parte (NEGATIVOS, ...)
     * @param escondidos quantos dos últimos filmes curtidos de cada pessoa esconder
     * @param maxPessoas limite de pessoas avaliadas (as primeiras por id, para dar sempre o mesmo grupo)
     */
    @Transactional(readOnly = true)
    public Resultado avaliar(Collection<String> desligar, int escondidos, int maxPessoas) {
        long inicio = System.currentTimeMillis();
        Set<ConnectionType> tipos = EnumSet.noneOf(ConnectionType.class);
        Set<Parte> partes = EnumSet.noneOf(Parte.class);
        for (String nome : desligar) {
            String n = nome.trim().toUpperCase(Locale.ROOT);
            if (n.isEmpty()) continue;
            try {
                tipos.add(ConnectionType.valueOf(n));
            } catch (IllegalArgumentException e) {
                try {
                    partes.add(Parte.valueOf(n));
                } catch (IllegalArgumentException e2) {
                    throw new IllegalArgumentException("Não sei desligar \"" + nome + "\". Use um ConnectionType "
                            + List.of(ConnectionType.values()) + " ou " + List.of(Parte.values()));
                }
            }
        }
        Ajustes ajustes = new Ajustes(
                new RecommendationEngine.Opcoes(!partes.contains(Parte.PESO_SEMENTES), !partes.contains(Parte.NEGATIVOS),
                        !partes.contains(Parte.NOTA_TMDB)),
                tipos,
                true); // a curtida da própria pessoa no filme escondido não pode ajudar a achá-lo

        MovieGraph grafo = graphCache.obter();
        Map<Long, List<Registro>> porPessoa = new TreeMap<>();
        for (Object[] linha : reviewRepository.findTodosRegistrosParaRecomendacao()) {
            porPessoa.computeIfAbsent((Long) linha[0], k -> new ArrayList<>()).add(new Registro((Long) linha[1],
                    (Double) linha[2], Boolean.TRUE.equals(linha[3]), (LocalDate) linha[4], (Instant) linha[5]));
        }
        Map<Long, TasteProfile> gostos = new HashMap<>();
        for (TasteProfile gosto : tasteProfileRepository.findAll()) gostos.put(gosto.getId(), gosto);
        Map<Long, List<Favorito>> favoritos = new HashMap<>();
        for (FavoritePerson f : favoritePersonRepository.findAll()) {
            favoritos.computeIfAbsent(f.getUser().getId(), k -> new ArrayList<>())
                    .add(new Favorito(f.getKind() == FavoritePerson.Kind.ACTOR, f.getName()));
        }

        int pessoas = 0;
        int comAcerto = 0;
        double somaRecall = 0;
        double somaMrr = 0;
        long somaRecomendacoes = 0;
        Set<Long> recomendadosNoTotal = new HashSet<>();

        for (Map.Entry<Long, List<Registro>> pessoa : porPessoa.entrySet()) {
            if (pessoas >= maxPessoas) break;
            List<Registro> registros = new ArrayList<>(pessoa.getValue());
            registros.sort(Comparator.comparing(Registro::data).reversed()
                    .thenComparing(Registro::createdAt, Comparator.reverseOrder()));

            // os últimos filmes curtidos que dá para recomendar (estão no grafo e já estrearam)
            LinkedHashSet<Long> curtidos = new LinkedHashSet<>();
            for (Registro r : registros) {
                Movie filme = grafo.getMovie(r.movieId());
                if (r.positivo() && filme != null && filme.jaEstreou()) curtidos.add(r.movieId());
            }
            if (curtidos.size() < escondidos + CURTIDAS_ALEM_DAS_ESCONDIDAS) continue;

            Set<Long> escondidosDaPessoa = new HashSet<>(curtidos.stream().limit(escondidos).toList());
            // o diário "de antes": nada dos escondidos nem do que veio depois do mais antigo deles
            LocalDate corte = registros.stream().filter(r -> escondidosDaPessoa.contains(r.movieId()))
                    .map(Registro::data).min(Comparator.naturalOrder()).orElseThrow();
            List<Registro> antes = registros.stream()
                    .filter(r -> !escondidosDaPessoa.contains(r.movieId()) && !r.data().isAfter(corte))
                    .toList();

            TasteProfile gosto = gostos.get(pessoa.getKey());
            List<Long> sementesDoQuestionario = gosto == null ? List.of()
                    : gosto.getSeedMovieIds().stream().filter(id -> !escondidosDaPessoa.contains(id)).toList();
            Preferencias preferencias = gosto == null ? null
                    : Preferencias.de(gosto.getGenres(), gosto.getAnswers());
            Entrada entrada = new Entrada(pessoa.getKey(), antes, sementesDoQuestionario, preferencias,
                    favoritos.getOrDefault(pessoa.getKey(), List.of()), List.of());

            List<RecommendationResult> recomendados = recommendationService.calcular(entrada, ajustes, grafo);
            int achados = 0;
            int primeiraPosicao = 0;
            for (int i = 0; i < Math.min(TOP_N, recomendados.size()); i++) {
                Long id = recomendados.get(i).getMovie().getId();
                recomendadosNoTotal.add(id);
                if (escondidosDaPessoa.contains(id)) {
                    achados++;
                    if (primeiraPosicao == 0) primeiraPosicao = i + 1;
                }
            }
            pessoas++;
            if (achados > 0) comAcerto++;
            somaRecall += (double) achados / escondidos;
            somaMrr += primeiraPosicao == 0 ? 0 : 1.0 / primeiraPosicao;
            somaRecomendacoes += Math.min(TOP_N, recomendados.size());
        }

        List<String> desligado = new ArrayList<>();
        tipos.forEach(t -> desligado.add(t.name()));
        partes.forEach(p -> desligado.add(p.name()));
        int catalogo = Math.max(1, grafo.getAllMovies().size());
        return new Resultado(pessoas, escondidos,
                arredondar(pessoas == 0 ? 0 : (double) comAcerto / pessoas),
                arredondar(pessoas == 0 ? 0 : somaRecall / pessoas),
                arredondar(pessoas == 0 ? 0 : somaMrr / pessoas),
                arredondar((double) recomendadosNoTotal.size() / catalogo),
                arredondar(pessoas == 0 ? 0 : (double) somaRecomendacoes / pessoas),
                desligado, System.currentTimeMillis() - inicio);
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 10000.0) / 10000.0;
    }
}
