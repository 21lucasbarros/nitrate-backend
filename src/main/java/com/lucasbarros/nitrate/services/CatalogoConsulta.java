package com.lucasbarros.nitrate.services;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Monta as consultas dos catálogos filtrados: o mesmo filtro serve para filmes (Movie e Review)
 * e para séries (Series e SeriesReview), que só mudam no nome das entidades e dos campos.
 * Devolve só os ids da página, na ordem; quem chama carrega as entidades.
 *
 * @param entidade  "Movie" ou "Series"
 * @param genero    condição de um gênero, com :genero no lugar do nome e t como o título
 * @param ano       campo do ano de lançamento
 * @param estreia   campo da data de lançamento (desempata dentro do ano)
 * @param titulo    campo do nome, para a ordem alfabética
 * @param review    entidade dos registros ("Review" ou "SeriesReview")
 * @param relacao   campo do registro que aponta para o título ("movie" ou "series")
 */
record CatalogoConsulta(String entidade, String genero, String ano, String estreia, String titulo,
                        String review, String relacao) {

    static final CatalogoConsulta FILMES = new CatalogoConsulta("Movie",
            "exists (select 1 from Movie m2 join m2.genres g where m2.id = t.id and g.name = :genero)",
            "releaseYear", "releaseDate", "title", "Review", "movie");

    static final CatalogoConsulta SERIES = new CatalogoConsulta("Series",
            "exists (select 1 from Series s2 join s2.genres g where s2.id = t.id and g = :genero)",
            "firstAirYear", "firstAirDate", "name", "SeriesReview", "series");

    record Pagina(List<Long> ids, long total) {}

    Pagina buscar(EntityManager em, CatalogoFiltro filtro, int pagina, int tamanho) {
        Map<String, Object> parametros = new HashMap<>();
        String onde = condicoes(filtro, parametros);

        // a média do Nitrate só entra quando o filtro ou a ordem precisam dela (é um join com os registros)
        boolean comunidade = filtro.ordem() == CatalogoFiltro.Ordem.COMUNIDADE;
        boolean comMedia = filtro.notaNitrateMin() != null || comunidade;
        StringBuilder ids = new StringBuilder("select t.id from ").append(entidade).append(" t ");
        if (comMedia) {
            ids.append("left join ").append(review).append(" r on r.").append(relacao).append(" = t and r.rating is not null ");
        }
        ids.append(onde);
        if (comMedia) {
            ids.append(" group by t.id");
            List<String> having = new ArrayList<>();
            if (filtro.notaNitrateMin() != null) having.add("avg(r.rating) >= :nitrate");
            if (comunidade) having.add("count(r.id) >= :avaliacoes");
            if (!having.isEmpty()) ids.append(" having ").append(String.join(" and ", having));
        }
        ids.append(" order by ").append(ordem(filtro.ordem())).append("t.id");

        TypedQuery<Long> consulta = em.createQuery(ids.toString(), Long.class)
                .setFirstResult(pagina * tamanho)
                .setMaxResults(tamanho);
        parametros.forEach(consulta::setParameter);
        if (filtro.notaNitrateMin() != null) consulta.setParameter("nitrate", filtro.notaNitrateMin());
        if (comunidade) consulta.setParameter("avaliacoes", CatalogoFiltro.AVALIACOES_MIN);

        StringBuilder contagem = new StringBuilder("select count(t) from ").append(entidade).append(" t ").append(onde);
        String notas = " from " + review + " r where r." + relacao + " = t and r.rating is not null)";
        if (filtro.notaNitrateMin() != null) {
            contagem.append(contagem.indexOf(" where ") < 0 ? " where " : " and ")
                    .append("(select avg(r.rating)").append(notas).append(" >= :nitrate");
        }
        if (comunidade) {
            contagem.append(contagem.indexOf(" where ") < 0 ? " where " : " and ")
                    .append("(select count(r.id)").append(notas).append(" >= :avaliacoes");
        }
        TypedQuery<Long> total = em.createQuery(contagem.toString(), Long.class);
        parametros.forEach(total::setParameter);
        if (filtro.notaNitrateMin() != null) total.setParameter("nitrate", filtro.notaNitrateMin());
        if (comunidade) total.setParameter("avaliacoes", CatalogoFiltro.AVALIACOES_MIN);

        return new Pagina(consulta.getResultList(), total.getSingleResult());
    }

    private String condicoes(CatalogoFiltro filtro, Map<String, Object> parametros) {
        List<String> partes = new ArrayList<>();
        for (int i = 0; i < filtro.generos().size(); i++) {
            partes.add(genero.replace(":genero", ":genero" + i));
            parametros.put("genero" + i, filtro.generos().get(i));
        }
        if (filtro.decada() != null) {
            partes.add("t." + ano + " between :anoDe and :anoAte");
            parametros.put("anoDe", filtro.decada());
            parametros.put("anoAte", filtro.decada() + 9);
        }
        if (filtro.notaTmdbMin() != null) {
            partes.add("t.rating >= :tmdb");
            parametros.put("tmdb", filtro.notaTmdbMin());
        }
        return partes.isEmpty() ? "" : "where " + String.join(" and ", partes);
    }

    /** termina com vírgula: o id sempre desempata, para a paginação não repetir nem pular títulos */
    private String ordem(CatalogoFiltro.Ordem ordem) {
        return switch (ordem) {
            case PADRAO -> "";
            case LANCAMENTO -> "t." + ano + " desc nulls last, t." + estreia + " desc nulls last, ";
            case LANCAMENTO_ANTIGOS -> "t." + ano + " asc nulls last, t." + estreia + " asc nulls last, ";
            case NOTA_TMDB -> "t.rating desc nulls last, ";
            // empate na média: o que mais gente avaliou primeiro
            case COMUNIDADE -> "avg(r.rating) desc, count(r.id) desc, ";
            case TITULO -> "lower(t." + titulo + ") asc, ";
        };
    }
}
