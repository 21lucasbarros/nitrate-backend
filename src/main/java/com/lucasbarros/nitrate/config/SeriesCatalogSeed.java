package com.lucasbarros.nitrate.config;

import com.lucasbarros.nitrate.repositories.SeriesRepository;
import com.lucasbarros.nitrate.services.SeriesService;
import com.lucasbarros.nitrate.tmdb.TmdbClient;
import com.lucasbarros.nitrate.tmdb.dto.TmdbTvSummaryDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Base do catálogo de séries: uma série só entra no banco quando alguém a abre, então o catálogo de
 * /series começava quase vazio. Uma vez em cada banco (marcado em tb_seed, como as listas oficiais),
 * importa as séries mais votadas do TMDB. As que já existem são puladas.
 */
@Component
public class SeriesCatalogSeed implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeriesCatalogSeed.class);

    private static final String SEED = "catalogo-series-200";
    private static final int PAGINAS = 10; // 20 por página: as 200 mais votadas
    private static final long PAUSA_MS = 150; // folga para o limite de requisições do TMDB

    @Autowired private TmdbClient tmdbClient;
    @Autowired private SeriesService seriesService;
    @Autowired private SeriesRepository seriesRepository;
    @Autowired private JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("create table if not exists tb_seed (name varchar(100) primary key, applied_at timestamp not null default now())");
        Integer feitas = jdbc.queryForObject("select count(*) from tb_seed where name = ?", Integer.class, SEED);
        if (feitas != null && feitas > 0) return;
        // cada série é uma chamada ao TMDB: em outra thread, para não atrasar a subida do servidor
        Thread.ofVirtual().name("catalogo-series").start(this::importar);
    }

    private void importar() {
        try {
            Set<Long> tmdbIds = new LinkedHashSet<>();
            for (int pagina = 1; pagina <= PAGINAS; pagina++) {
                for (TmdbTvSummaryDTO resumo : tmdbClient.seriesMaisVotadas(pagina).getResults()) {
                    tmdbIds.add(resumo.getId());
                }
            }
            int novas = 0;
            int falhas = 0;
            for (Long tmdbId : tmdbIds) {
                if (seriesRepository.findByTmdbId(tmdbId).isPresent()) continue;
                try {
                    seriesService.obterOuImportarId(tmdbId);
                    novas++;
                    Thread.sleep(PAUSA_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception e) {
                    // uma série que falha não derruba as outras
                    falhas++;
                    log.warn("não foi possível importar a série {} do TMDB: {}", tmdbId, e.getMessage());
                }
            }
            // com muitas falhas (TMDB fora do ar), tenta de novo na próxima subida
            if (falhas > tmdbIds.size() / 10) {
                log.warn("catálogo de séries: {} importadas e {} falhas; tenta de novo na próxima subida", novas, falhas);
                return;
            }
            jdbc.update("insert into tb_seed (name) values (?)", SEED);
            log.info("catálogo de séries: {} séries importadas do TMDB ({} já estavam no banco)", novas, tmdbIds.size() - novas - falhas);
        } catch (Exception e) {
            log.warn("não foi possível montar o catálogo de séries agora; tenta de novo na próxima subida", e);
        }
    }
}
