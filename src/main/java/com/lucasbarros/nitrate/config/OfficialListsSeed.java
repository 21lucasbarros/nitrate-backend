package com.lucasbarros.nitrate.config;

import com.lucasbarros.nitrate.dto.MovieListRequestDTO;
import com.lucasbarros.nitrate.dto.MovieListRequestDTO.Item;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.services.MovieListService;
import com.lucasbarros.nitrate.services.MovieService;
import com.lucasbarros.nitrate.services.NitrateAccountService;
import com.lucasbarros.nitrate.services.SeriesService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Listas do Nitrate criadas pelo próprio servidor, uma vez em cada banco: tb_seed guarda quais já foram
 * criadas, então uma lista apagada pelo admin não volta. Depois de criadas, o admin cuida delas pelo
 * painel. Os filmes e séries vêm do TMDB pelo id, importados se ainda não estiverem no acervo.
 */
@Component
public class OfficialListsSeed implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OfficialListsSeed.class);

    private record Titulo(boolean serie, long tmdbId) {}

    private static Titulo filme(long tmdbId) { return new Titulo(false, tmdbId); }
    private static Titulo serie(long tmdbId) { return new Titulo(true, tmdbId); }

    private static final String VINGADORES_SEED = "lista-vingadores-doomsday";
    private static final String VINGADORES_NOME = "Para ver antes de Vingadores: Doomsday";
    private static final String VINGADORES_DESCRICAO =
            "Uma seleção de 15 títulos para chegar em dia ao próximo filme dos Vingadores, dos X-Men de 2000 "
            + "ao Quarteto Fantástico. Loki entra com a 1ª e a 2ª temporadas: é a única série da lista.";
    private static final List<Titulo> VINGADORES = List.of(
            filme(36657),  // X-Men (2000)
            filme(36658),  // X-Men 2 (2003)
            filme(1771),   // Capitão América: O Primeiro Vingador
            filme(24428),  // Os Vingadores
            filme(299536), // Vingadores: Guerra Infinita
            filme(299534), // Vingadores: Ultimato
            serie(84958),  // Loki (1ª e 2ª temporadas)
            filme(566525), // Shang-Chi e a Lenda dos Dez Anéis
            filme(634649), // Homem-Aranha: Sem Volta Para Casa
            filme(505642), // Pantera Negra: Wakanda Para Sempre
            filme(822119), // Capitão América: Admirável Mundo Novo
            filme(453395), // Doutor Estranho no Multiverso da Loucura
            filme(533535), // Deadpool & Wolverine
            filme(986056), // Thunderbolts*
            filme(617126)  // Quarteto Fantástico: Primeiros Passos
    );

    @Autowired private UserRepository userRepository;
    @Autowired private NitrateAccountService nitrateAccountService;
    @Autowired private MovieService movieService;
    @Autowired private SeriesService seriesService;
    @Autowired private MovieListService movieListService;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private JdbcTemplate jdbc;

    @Override
    public void run(ApplicationArguments args) {
        jdbc.execute("create table if not exists tb_seed (name varchar(100) primary key, applied_at timestamp not null default now())");
        Integer feitas = jdbc.queryForObject("select count(*) from tb_seed where name = ?", Integer.class, VINGADORES_SEED);
        if (feitas != null && feitas > 0) return;
        // o @nitrate é de outra pessoa (de antes de ser reservado): não cria nada até a conta ser renomeada
        if (userRepository.findFirstByUsernameIgnoreCase(NitrateAccountService.USERNAME)
                .filter(u -> !NitrateAccountService.ehConta(u)).isPresent()) return;
        // as importações chamam o TMDB: em outra thread, para não atrasar a subida do servidor
        Thread.ofVirtual().name("listas-oficiais").start(this::criar);
    }

    private void criar() {
        try {
            // primeiro os títulos (rede); se o TMDB falhar, nada é criado e tenta de novo na próxima subida
            List<Item> itens = new ArrayList<>();
            for (Titulo t : VINGADORES) {
                itens.add(t.serie()
                        ? new Item("SERIES", seriesService.obterOuImportarId(t.tmdbId()))
                        : new Item("MOVIE", movieService.obterOuImportarId(t.tmdbId())));
            }
            // conta (se ainda não existir), lista e a marca de feito juntas: ou tudo, ou nada
            transactionTemplate.executeWithoutResult(status -> {
                movieListService.criar(nitrateAccountService.conta(),
                        new MovieListRequestDTO(VINGADORES_NOME, VINGADORES_DESCRICAO, true, null, itens));
                jdbc.update("insert into tb_seed (name) values (?)", VINGADORES_SEED);
            });
            log.info("listas do Nitrate criadas: {}", VINGADORES_NOME);
        } catch (Exception e) {
            log.warn("não foi possível criar as listas do Nitrate agora; tenta de novo na próxima subida", e);
        }
    }
}
