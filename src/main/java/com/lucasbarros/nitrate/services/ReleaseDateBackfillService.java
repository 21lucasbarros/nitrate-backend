package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.tmdb.TmdbClient;
import com.lucasbarros.nitrate.tmdb.TmdbImportService;
import com.lucasbarros.nitrate.tmdb.dto.TmdbMovieDetailDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A data de estreia passou a ser salva depois que boa parte do acervo já existia: esses filmes
 * só têm o ano, e a aba "Filmes" (ordem por lançamento) empatava todos do mesmo ano. Aqui
 * buscamos a data no TMDB para os que estão sem ela, um filme por vez e salvando a cada um,
 * então dá para interromper e continuar depois (a próxima rodada pega só o que faltou).
 */
@Service
public class ReleaseDateBackfillService {

    private static final Logger log = LoggerFactory.getLogger(ReleaseDateBackfillService.class);

    @Autowired private MovieRepository movieRepository;
    @Autowired private TmdbClient tmdbClient;

    public record ResultadoDatas(int filmesVerificados, int datasPreenchidas, int filmesRestantes) {}

    /** roda em segundo plano (na inicialização), sem segurar a subida da API */
    @Async
    public void preencherEmSegundoPlano(Duration orcamento) {
        try {
            ResultadoDatas resultado = preencher(orcamento);
            if (resultado.filmesVerificados() > 0) {
                log.info("datas de estreia: {} verificado(s), {} preenchida(s), {} restante(s)",
                        resultado.filmesVerificados(), resultado.datasPreenchidas(), resultado.filmesRestantes());
            }
        } catch (Exception e) {
            log.warn("datas de estreia: falha no preenchimento em segundo plano: {}", e.getMessage());
        }
    }

    public ResultadoDatas preencher(Duration orcamento) {
        Instant limite = Instant.now().plus(orcamento);
        // os que estão em algum diário primeiro; o resto do acervo depois
        Map<Long, Long> pendentes = new LinkedHashMap<>();
        for (Object[] linha : movieRepository.findSemDataDeEstreiaEmDiarios()) pendentes.put((Long) linha[0], (Long) linha[1]);
        for (Object[] linha : movieRepository.findSemDataDeEstreia()) pendentes.putIfAbsent((Long) linha[0], (Long) linha[1]);

        int verificados = 0;
        int preenchidas = 0;
        for (Map.Entry<Long, Long> filme : pendentes.entrySet()) {
            if (Instant.now().isAfter(limite)) break;
            Long id = filme.getKey();
            Long tmdbId = filme.getValue();
            try {
                TmdbMovieDetailDTO detalhe = tmdbClient.buscarDetalhes(tmdbId);
                LocalDate estreia = detalhe == null ? null : TmdbImportService.extrairData(detalhe.getReleaseDate());
                if (estreia != null) {
                    movieRepository.atualizarDataDeEstreia(id, estreia);
                    preenchidas++;
                }
            } catch (Exception e) {
                log.warn("datas de estreia: falha no filme {} (tmdb {}): {}", id, tmdbId, e.getMessage());
            }
            verificados++;
        }
        return new ResultadoDatas(verificados, preenchidas, pendentes.size() - verificados);
    }
}
