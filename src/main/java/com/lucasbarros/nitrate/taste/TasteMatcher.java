package com.lucasbarros.nitrate.taste;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.taste.TasteQuiz.GenreOption;
import com.lucasbarros.nitrate.taste.TasteQuiz.Option;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Pontua filmes do acervo conforme as respostas do questionário de gostos. Usado para a grade de
 * filmes do onboarding e pelas recomendações (empurrão e plano B de quem ainda não curtiu nada).
 *
 * Cada preferência vira uma "fila" de filmes: uma por resposta de "como" e uma por gênero escolhido
 * cuja pergunta ficou sem resposta. Quem escolheu Comédia e respondeu "comédia romântica" não ganha
 * a fila de qualquer comédia: a resposta refina o gênero.
 */
@Component
public class TasteMatcher {

    // na grade, cada resposta ganha dois lugares para cada um de um gênero solto
    private static final int VEZ_RESPOSTA = 2;
    private static final int VEZ_GENERO = 1;
    // cada gênero do filme fora do que a resposta pede custa isto: "comédia romântica" pura vem
    // antes de um drama de três horas que também tem romance
    private static final double PUREZA = 0.72;
    // quanto os registros no Nitrate pesam: ajuda a mostrar filmes conhecidos, sem passar por cima
    // da relevância (com peso 1, um filme muito registrado valia 4x um que combina mais)
    private static final double PESO_POPULARIDADE = 0.25;
    private static final int MAX_POR_DIRETOR = 2;
    private static final int MAX_POR_FRANQUIA = 2;
    private static final Duration VALIDADE_POPULARIDADE = Duration.ofMinutes(10);
    private static final String POPULARES = "*";
    private static final double SEM_TITULO_TRADUZIDO = 0.3;
    private static final Pattern LETRA_LATINA = Pattern.compile("\\p{IsLatin}");

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private PalavrasChave palavrasChave;

    private volatile Map<Long, Long> registrosPorFilme = Map.of();
    private volatile Instant popularidadeEm = Instant.EPOCH;

    /** respostas já validadas (só ids que existem no questionário) */
    public record Preferencias(Set<String> genres, Set<String> answers) {

        public static Preferencias de(Collection<String> genres, Collection<String> answers) {
            return new Preferencias(
                    validos(genres, TasteQuiz.GENERO_POR_ID.keySet()),
                    validos(answers, TasteQuiz.OPCAO_POR_ID.keySet()));
        }

        public boolean vazia() {
            return genres.isEmpty() && answers.isEmpty();
        }

        /** respostas na ordem do questionário (os Sets não têm ordem) */
        List<Option> respostas() {
            return TasteQuiz.OPCOES.stream().filter(o -> answers.contains(o.id())).toList();
        }

        /** gêneros escolhidos cuja pergunta de "como" não teve resposta */
        List<GenreOption> generosSoltos() {
            Set<String> respondidos = answers.stream().map(TasteQuiz.GENERO_DA_OPCAO::get).collect(Collectors.toSet());
            return TasteQuiz.GENEROS.stream()
                    .filter(g -> genres.contains(g.id()) && !respondidos.contains(g.id()))
                    .toList();
        }

        private static Set<String> validos(Collection<String> ids, Set<String> existentes) {
            if (ids == null) return Set.of();
            return ids.stream().filter(Objects::nonNull).map(String::trim).filter(existentes::contains)
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    /** filmes de uma preferência, para a grade mostrar por que estão ali */
    public record Grupo(String id, String label, List<Movie> filmes) {
    }

    private record Pontuado(Movie filme, double pontos) {
    }

    private record Fila(String id, String label, int vez, List<Pontuado> filmes) {
    }

    /** a grade inteira numa lista só, na ordem do rodízio */
    public List<Movie> melhores(Preferencias prefs, Collection<Movie> filmes, Set<Long> excluir, int limite) {
        return escolher(prefs, filmes, excluir, limite).stream().map(Map.Entry::getValue).toList();
    }

    /**
     * A grade separada por preferência. Os filmes vêm em rodízio entre as filas (cada resposta
     * ganha espaço, em vez de a mais comum ocupar tudo), sem repetir e com no máximo dois por
     * diretor ou franquia. Sem nenhuma preferência, um grupo só com os mais registrados.
     */
    public List<Grupo> grupos(Preferencias prefs, Collection<Movie> filmes, Set<Long> excluir, int limite) {
        Map<String, Fila> filas = new LinkedHashMap<>();
        filas(prefs, filmes, excluir).forEach(f -> filas.put(f.id(), f));
        Map<String, List<Movie>> porFila = new LinkedHashMap<>();
        filas.keySet().forEach(id -> porFila.put(id, new ArrayList<>()));
        for (Map.Entry<String, Movie> escolhido : escolher(prefs, filmes, excluir, limite)) {
            porFila.get(escolhido.getKey()).add(escolhido.getValue());
        }
        return porFila.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .map(e -> new Grupo(e.getKey(), filas.get(e.getKey()).label(), e.getValue()))
                .toList();
    }

    /** o filme combina com alguma resposta ou gênero escolhido */
    public boolean combina(Preferencias prefs, Movie filme) {
        return !motivos(prefs, filme).isEmpty();
    }

    /** "comédia romântica", "suspense"...: o que o filme tem de cada preferência, as respostas antes */
    public List<String> motivos(Preferencias prefs, Movie filme) {
        List<String> motivos = new ArrayList<>();
        for (Option opcao : prefs.respostas()) {
            if (opcao.filtro().aceita(filme, palavrasChave)) motivos.add(opcao.motivo());
        }
        if (motivos.isEmpty()) {
            for (GenreOption genero : prefs.generosSoltos()) {
                if (genero.aceita(filme, palavrasChave)) motivos.add(genero.label().toLowerCase());
            }
        }
        return motivos;
    }

    /** pares (fila, filme) na ordem do rodízio com peso */
    private List<Map.Entry<String, Movie>> escolher(Preferencias prefs, Collection<Movie> filmes, Set<Long> excluir,
                                                    int limite) {
        List<Fila> filas = filas(prefs, filmes, excluir);
        List<Map.Entry<String, Movie>> escolhidos = new ArrayList<>();
        Set<Long> jaEscolhidos = new HashSet<>();
        Map<String, Integer> porDiretor = new HashMap<>();
        Map<String, Integer> porFranquia = new HashMap<>();
        int[] posicao = new int[filas.size()];
        boolean avancou = true;
        while (escolhidos.size() < limite && avancou) {
            avancou = false;
            for (int i = 0; i < filas.size() && escolhidos.size() < limite; i++) {
                Fila fila = filas.get(i);
                for (int vez = 0; vez < fila.vez() && escolhidos.size() < limite; vez++) {
                    while (posicao[i] < fila.filmes().size()) {
                        Movie filme = fila.filmes().get(posicao[i]++).filme();
                        if (jaEscolhidos.contains(filme.getId()) || !cabe(filme, porDiretor, porFranquia)) continue;
                        jaEscolhidos.add(filme.getId());
                        escolhidos.add(Map.entry(fila.id(), filme));
                        avancou = true;
                        break;
                    }
                }
            }
        }
        return escolhidos;
    }

    /** uma fila por resposta e por gênero solto, cada uma do filme que mais combina para o que menos */
    private List<Fila> filas(Preferencias prefs, Collection<Movie> filmes, Set<Long> excluir) {
        Map<Long, Long> registros = popularidade();
        List<Movie> candidatos = filmes.stream()
                .filter(f -> !excluir.contains(f.getId()) && f.getPosterPath() != null)
                .toList();

        List<Fila> filas = new ArrayList<>();
        for (Option opcao : prefs.respostas()) {
            // com palavra-chave (musical), a mistura de gêneros não diz nada: sem pureza
            Set<String> pedidos = opcao.filtro().palavraChave() != null ? null : opcao.filtro().generosDaResposta();
            List<Pontuado> lista = candidatos.stream()
                    .filter(f -> opcao.filtro().aceita(f, palavrasChave))
                    .map(f -> new Pontuado(f, nota(f, pedidos, prefs, registros)))
                    .toList();
            filas.add(new Fila(opcao.id(), opcao.label(), VEZ_RESPOSTA, ordenar(lista)));
        }
        for (GenreOption genero : prefs.generosSoltos()) {
            // o filme pode misturar os gêneros que a pessoa escolheu sem perder pontos
            Set<String> escolhidos = genero.palavraChave() != null ? null : prefs.genres().stream()
                    .map(id -> TasteQuiz.GENERO_POR_ID.get(id).nome()).collect(Collectors.toSet());
            List<Pontuado> lista = candidatos.stream()
                    .filter(f -> genero.aceita(f, palavrasChave))
                    .map(f -> new Pontuado(f, nota(f, escolhidos, prefs, registros)))
                    .toList();
            filas.add(new Fila(genero.id(), genero.label(), VEZ_GENERO, ordenar(lista)));
        }
        if (prefs.vazia()) {
            List<Pontuado> lista = candidatos.stream()
                    .map(f -> new Pontuado(f, nota(f, null, prefs, registros)))
                    .toList();
            filas.add(new Fila(POPULARES, "Populares no Nitrate", VEZ_GENERO, ordenar(lista)));
        }
        // as filas que começam com os filmes mais fortes abrem o rodízio
        filas.removeIf(f -> f.filmes().isEmpty());
        filas.sort(Comparator.comparingDouble((Fila f) -> f.filmes().get(0).pontos()).reversed());
        return filas;
    }

    /**
     * Pureza (gêneros fora de `pedidos` custam pontos; nulo = sem pureza), nota do TMDB e
     * registros no Nitrate. A grade precisa de filmes que a pessoa provavelmente viu e que sejam
     * mesmo do tipo que ela escolheu.
     */
    private double nota(Movie filme, Set<String> pedidos, Preferencias prefs, Map<Long, Long> registros) {
        double pontos = 1;
        if (pedidos != null) {
            long fora = TasteQuiz.generosDe(filme).stream().filter(g -> !pedidos.contains(g)).count();
            pontos *= Math.pow(PUREZA, fora);
        }
        // sem título em português (só "愛のぬくもり"), quase ninguém aqui viu: a grade precisa de conhecidos
        if (filme.getTitle() != null && !LETRA_LATINA.matcher(filme.getTitle()).find()) {
            pontos *= SEM_TITULO_TRADUZIDO;
        }
        double nota = filme.getRating() == null ? 0.6 : Math.max(0.3, filme.getRating() / 10.0);
        double popular = 1 + PESO_POPULARIDADE * Math.log1p(registros.getOrDefault(filme.getId(), 0L));
        return pontos * nota * popular;
    }

    private static List<Pontuado> ordenar(List<Pontuado> lista) {
        List<Pontuado> ordenada = new ArrayList<>(lista);
        ordenada.sort(Comparator.comparingDouble(Pontuado::pontos).reversed());
        return ordenada;
    }

    private static boolean cabe(Movie filme, Map<String, Integer> porDiretor, Map<String, Integer> porFranquia) {
        String diretor = filme.getDirector() != null ? filme.getDirector().getName().toLowerCase() : null;
        String franquia = filme.getFranchise() != null ? filme.getFranchise().getName().toLowerCase() : null;
        if (diretor != null && porDiretor.getOrDefault(diretor, 0) >= MAX_POR_DIRETOR) return false;
        if (franquia != null && porFranquia.getOrDefault(franquia, 0) >= MAX_POR_FRANQUIA) return false;
        if (diretor != null) porDiretor.merge(diretor, 1, Integer::sum);
        if (franquia != null) porFranquia.merge(franquia, 1, Integer::sum);
        return true;
    }

    /** contagem de registros por filme, recalculada a cada VALIDADE_POPULARIDADE */
    private Map<Long, Long> popularidade() {
        if (popularidadeEm.plus(VALIDADE_POPULARIDADE).isBefore(Instant.now())) {
            Map<Long, Long> contagem = new HashMap<>();
            for (Object[] linha : reviewRepository.countPorFilme()) {
                contagem.put((Long) linha[0], (Long) linha[1]);
            }
            registrosPorFilme = Map.copyOf(contagem);
            popularidadeEm = Instant.now();
        }
        return registrosPorFilme;
    }
}
