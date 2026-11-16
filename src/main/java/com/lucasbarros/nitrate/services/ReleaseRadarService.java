package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Notification;
import com.lucasbarros.nitrate.entities.WatchlistItem;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.repositories.NotificationRepository;
import com.lucasbarros.nitrate.repositories.WatchlistRepository;
import com.lucasbarros.nitrate.tmdb.TmdbClient;
import com.lucasbarros.nitrate.tmdb.TmdbImportService;
import com.lucasbarros.nitrate.tmdb.dto.TmdbReleaseDatesDTO;
import com.lucasbarros.nitrate.tmdb.dto.TmdbWatchProvidersDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Radar de Estreias: uma vez por dia (Cron Job da Vercel em /admin/release-radar), confere os
 * filmes que estão em alguma watchlist e ainda não estrearam em todo lugar. Quando um deles
 * chega aos cinemas do Brasil ou a um streaming por assinatura daqui, avisa quem tem o filme
 * na watchlist.
 *
 * O aviso nasce da mudança de estado entre duas verificações (não estava → está). Assim, um
 * filme antigo que já está na Netflix não gera aviso só por ter entrado na watchlist hoje.
 */
@Service
public class ReleaseRadarService {

    private static final Logger log = LoggerFactory.getLogger(ReleaseRadarService.class);

    private static final String PAIS = "BR";
    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    // depois disso o filme deixa de ser "lançamento" e sai do radar
    private static final int DIAS_NO_RADAR_APOS_ESTREIA = 120;
    // na primeira verificação não há estado anterior: avisa da estreia nos cinemas só se ela foi agora
    private static final int DIAS_ESTREIA_RECENTE = 7;
    private static final int SERVICOS_NO_AVISO = 3;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.of("pt", "BR"));

    @Autowired private WatchlistRepository watchlistRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private TmdbClient tmdbClient;

    public record ResultadoRadar(int filmesVerificados, int avisosCriados, int filmesRestantes) {}

    public ResultadoRadar verificar(Duration orcamento) {
        Instant limite = Instant.now().plus(orcamento);
        LocalDate hoje = LocalDate.now(FUSO);
        List<Long> ids = watchlistRepository.findIdsParaRadar(hoje.minusDays(DIAS_NO_RADAR_APOS_ESTREIA));

        int verificados = 0;
        int avisos = 0;
        for (Long id : ids) {
            if (Instant.now().isAfter(limite)) break;
            try {
                avisos += verificarFilme(id, hoje);
            } catch (Exception e) {
                log.warn("Radar: falha ao verificar o filme {}: {}", id, e.getMessage());
            }
            verificados++;
        }
        return new ResultadoRadar(verificados, avisos, ids.size() - verificados);
    }

    /** @return quantos avisos foram criados */
    private int verificarFilme(Long movieId, LocalDate hoje) {
        Movie filme = movieRepository.findById(movieId).orElse(null);
        if (filme == null || filme.getTmdbId() == null) return 0;

        LocalDate estreiaBr = estreiaNosCinemasDoBrasil(filme.getTmdbId());
        if (estreiaBr != null) filme.setReleaseDate(estreiaBr);

        boolean nosCinemas = filme.getReleaseDate() != null && !filme.getReleaseDate().isAfter(hoje);
        List<String> streaming = streamingNoBrasil(filme.getTmdbId());
        boolean noStreaming = !streaming.isEmpty();

        int avisos = 0;
        Boolean antesNosCinemas = filme.getRadarNosCinemas();
        if (nosCinemas && !Boolean.TRUE.equals(antesNosCinemas)) {
            boolean estreiaRecente = filme.getReleaseDate().isAfter(hoje.minusDays(DIAS_ESTREIA_RECENTE));
            if (antesNosCinemas != null || estreiaRecente) {
                avisos += avisar(filme, Notification.Type.CINEMA, "Estreou em " + filme.getReleaseDate().format(DATA));
            }
        }
        // sem estado anterior não dá para saber se chegou agora; só registra
        if (noStreaming && Boolean.FALSE.equals(filme.getRadarNoStreaming())) {
            String servicos = String.join(", ", streaming.subList(0, Math.min(SERVICOS_NO_AVISO, streaming.size())));
            avisos += avisar(filme, Notification.Type.STREAMING, servicos);
        }

        movieRepository.atualizarRadar(filme.getId(), filme.getReleaseDate(), nosCinemas, noStreaming);
        return avisos;
    }

    private int avisar(Movie filme, Notification.Type tipo, String detalhe) {
        int criados = 0;
        for (WatchlistItem item : watchlistRepository.findByMovie(filme)) {
            if (!notificationRepository.existsByUserAndMovieAndType(item.getUser(), filme, tipo)) {
                notificationRepository.save(new Notification(item.getUser(), filme, tipo, detalhe));
                criados++;
            }
        }
        return criados;
    }

    /** a primeira data de cinema (normal ou limitado) no Brasil, se o TMDB tiver */
    private LocalDate estreiaNosCinemasDoBrasil(Long tmdbId) {
        TmdbReleaseDatesDTO datas = tmdbClient.buscarDatasDeLancamento(tmdbId);
        if (datas == null) return null;
        return datas.getResults().stream()
                .filter(c -> PAIS.equals(c.getPais()))
                .flatMap(c -> c.getLancamentos().stream())
                .filter(l -> l.getType() == TmdbReleaseDatesDTO.CINEMA || l.getType() == TmdbReleaseDatesDTO.CINEMA_LIMITADO)
                .map(l -> TmdbImportService.extrairData(l.getData()))
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    /** serviços por assinatura (e grátis) no Brasil, na ordem de destaque do JustWatch */
    private List<String> streamingNoBrasil(Long tmdbId) {
        TmdbWatchProvidersDTO resposta = tmdbClient.buscarOndeAssistir(tmdbId);
        TmdbWatchProvidersDTO.Country brasil = resposta == null ? null : resposta.getResults().get(PAIS);
        if (brasil == null) return List.of();
        return Stream.of(brasil.getFlatrate(), brasil.getFree(), brasil.getAds())
                .flatMap(List::stream)
                .sorted(Comparator.comparingInt(TmdbWatchProvidersDTO.Provider::getDisplayPriority))
                .map(TmdbWatchProvidersDTO.Provider::getProviderName)
                .distinct()
                .toList();
    }
}
