package com.lucasbarros.nitrate.services;

import java.util.List;
import java.util.Locale;

/**
 * Filtros dos catálogos de filmes e de séries (páginas /movies e /series).
 *
 * @param generos        todos precisam estar no título ("Terror" e "Comédia" = comédia de terror)
 * @param decada         1990 = de 1990 a 1999; nulo = qualquer ano
 * @param notaTmdbMin    nota do TMDB (0 a 10)
 * @param notaNitrateMin média das notas dadas no Nitrate (0,5 a 5); sem nenhuma nota, o título não entra
 */
public record CatalogoFiltro(List<String> generos, Integer decada, Double notaTmdbMin, Double notaNitrateMin, Ordem ordem) {

    private static final int GENEROS_MAX = 5;

    /** mesmo critério da antiga fileira "Mais bem avaliados pela comunidade" */
    public static final long AVALIACOES_MIN = 2;

    public enum Ordem {
        // PADRAO: a ordem em que os títulos entraram no Nitrate (a de antes dos filtros).
        // COMUNIDADE: maior média no Nitrate, só entre os que várias pessoas avaliaram (ver AVALIACOES_MIN)
        PADRAO, LANCAMENTO, LANCAMENTO_ANTIGOS, NOTA_TMDB, COMUNIDADE, TITULO;

        /** "nota-tmdb" -> NOTA_TMDB; valor desconhecido cai no padrão */
        public static Ordem de(String valor) {
            if (valor == null || valor.isBlank()) return PADRAO;
            try {
                return valueOf(valor.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
            } catch (IllegalArgumentException e) {
                return PADRAO;
            }
        }
    }

    public static CatalogoFiltro de(List<String> generos, Integer decada, Double notaTmdbMin, Double notaNitrateMin, String ordem) {
        List<String> limpos = generos == null ? List.of() : generos.stream()
                .filter(g -> g != null && !g.isBlank())
                .map(String::trim)
                .distinct()
                .limit(GENEROS_MAX)
                .toList();
        // valores fora da escala são ignorados em vez de devolver um catálogo vazio
        Integer decadaValida = decada != null && decada >= 1870 && decada <= 2100 ? decada - decada % 10 : null;
        Double tmdb = notaTmdbMin != null && notaTmdbMin > 0 && notaTmdbMin <= 10 ? notaTmdbMin : null;
        Double nitrate = notaNitrateMin != null && notaNitrateMin > 0 && notaNitrateMin <= 5 ? notaNitrateMin : null;
        return new CatalogoFiltro(limpos, decadaValida, tmdb, nitrate, Ordem.de(ordem));
    }
}
