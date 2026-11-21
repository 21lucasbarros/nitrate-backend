package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.SeriesSearchResultDTO;
import com.lucasbarros.nitrate.entities.Notification;
import com.lucasbarros.nitrate.entities.Series;
import com.lucasbarros.nitrate.entities.SeriesWatchlistItem;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.NotificationRepository;
import com.lucasbarros.nitrate.repositories.SeriesRepository;
import com.lucasbarros.nitrate.repositories.SeriesReviewRepository;
import com.lucasbarros.nitrate.repositories.SeriesWatchlistRepository;
import com.lucasbarros.nitrate.tmdb.TmdbClient;
import com.lucasbarros.nitrate.tmdb.TmdbImportService;
import com.lucasbarros.nitrate.tmdb.dto.TmdbTvDetailDTO;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Radar de Estreias das séries: junto com o dos filmes (Cron Job em /admin/release-radar),
 * confere as séries que alguém tem na watchlist ou já registrou e avisa quando
 * - o primeiro episódio vai ao ar (SERIES_PREMIERE), ou
 * - uma temporada nova estreia numa série que segue no ar (SERIES_SEASON).
 *
 * Como nos filmes, o aviso nasce da mudança de estado entre duas verificações: uma série antiga
 * (ou uma temporada antiga) não gera aviso só porque a série entrou na watchlist hoje.
 */
@Service
public class SeriesRadarService {

    private static final Logger log = LoggerFactory.getLogger(SeriesRadarService.class);

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    // sem estado anterior, só avisa do que estreou nesses últimos dias; o resto já era "velho"
    private static final int DIAS_ESTREIA_RECENTE = 7;
    private static final int CANAIS_NO_AVISO = 2;
    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("d 'de' MMMM", Locale.of("pt", "BR"));

    @Autowired private SeriesRepository seriesRepository;
    @Autowired private SeriesWatchlistRepository seriesWatchlistRepository;
    @Autowired private SeriesReviewRepository seriesReviewRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private TmdbClient tmdbClient;

    public record ResultadoRadar(int seriesVerificadas, int avisosCriados, int seriesRestantes) {}

    public ResultadoRadar verificar(Duration orcamento) {
        Instant limite = Instant.now().plus(orcamento);
        LocalDate hoje = LocalDate.now(FUSO);
        List<Long> ids = seriesRepository.findIdsParaRadar(hoje.minusDays(DIAS_ESTREIA_RECENTE));

        int verificadas = 0;
        int avisos = 0;
        for (Long id : ids) {
            if (Instant.now().isAfter(limite)) break;
            try {
                avisos += verificarSerie(id, hoje);
            } catch (Exception e) {
                log.warn("Radar: falha ao verificar a série {}: {}", id, e.getMessage());
            }
            verificadas++;
        }
        return new ResultadoRadar(verificadas, avisos, ids.size() - verificadas);
    }

    /** @return quantos avisos foram criados */
    private int verificarSerie(Long seriesId, LocalDate hoje) {
        Series serie = seriesRepository.findById(seriesId).orElse(null);
        if (serie == null) return 0;

        TmdbTvDetailDTO d = tmdbClient.buscarSerieSemElenco(serie.getTmdbId());
        if (d == null) return 0;
        LocalDate estreia = TmdbImportService.extrairData(d.getFirstAirDate());
        boolean estreou = estreia != null && !estreia.isAfter(hoje);
        TmdbTvDetailDTO.Season ultima = ultimaTemporadaNoAr(d, hoje);
        Integer numeroUltima = ultima == null ? null : ultima.getSeasonNumber();

        int avisos = 0;
        if (estreou && !Boolean.TRUE.equals(serie.getRadarEstreou())) {
            if (Boolean.FALSE.equals(serie.getRadarEstreou()) || recente(estreia, hoje)) {
                avisos += avisarEstreia(serie, detalhe("Estreou em " + estreia.format(DATA), d));
            }
        } else if (estreou && numeroUltima != null && numeroUltima > 1) {
            // a temporada 1 é a própria estreia da série
            Integer antes = serie.getRadarUltimaTemporada();
            LocalDate dataTemporada = TmdbImportService.extrairData(ultima.getAirDate());
            boolean nova = antes != null ? numeroUltima > antes : recente(dataTemporada, hoje);
            if (nova) {
                String texto = "Temporada " + numeroUltima
                        + (dataTemporada == null ? "" : " · estreou em " + dataTemporada.format(DATA));
                avisos += avisarTemporada(serie, numeroUltima, detalhe(texto, d));
            }
        }

        boolean noAr = SeriesService.emExibicao(d);
        seriesRepository.atualizarRadar(serie.getId(), estreia, estreou, numeroUltima, d.getStatus(),
                noAr ? null : SeriesSearchResultDTO.ano(d.getLastAirDate()), Instant.now());
        return avisos;
    }

    /** a maior temporada (fora os especiais, que são a 0) cujo primeiro episódio já foi ao ar */
    private static TmdbTvDetailDTO.Season ultimaTemporadaNoAr(TmdbTvDetailDTO d, LocalDate hoje) {
        return d.getSeasons().stream()
                .filter(t -> t.getSeasonNumber() != null && t.getSeasonNumber() > 0)
                .filter(t -> {
                    LocalDate data = TmdbImportService.extrairData(t.getAirDate());
                    return data != null && !data.isAfter(hoje);
                })
                .max(Comparator.comparing(TmdbTvDetailDTO.Season::getSeasonNumber))
                .orElse(null);
    }

    private static boolean recente(LocalDate data, LocalDate hoje) {
        return data != null && data.isAfter(hoje.minusDays(DIAS_ESTREIA_RECENTE));
    }

    /** "Estreou em 26 de março · Prime Video" */
    private static String detalhe(String texto, TmdbTvDetailDTO d) {
        List<String> canais = d.getNetworks().stream().map(TmdbTvDetailDTO.Network::getName)
                .filter(Objects::nonNull).distinct().limit(CANAIS_NO_AVISO).toList();
        if (!canais.isEmpty()) texto += " · " + String.join(", ", canais);
        return texto.length() > 200 ? texto.substring(0, 200) : texto;
    }

    private int avisarEstreia(Series serie, String detalhe) {
        int criados = 0;
        for (User user : interessados(serie)) {
            if (!notificationRepository.existsByUserAndSeriesAndType(user, serie, Notification.Type.SERIES_PREMIERE)) {
                notificationRepository.save(Notification.estreiaDeSerie(user, serie, detalhe));
                criados++;
            }
        }
        return criados;
    }

    private int avisarTemporada(Series serie, int temporada, String detalhe) {
        int criados = 0;
        for (User user : interessados(serie)) {
            if (!notificationRepository.existsByUserAndSeriesAndTypeAndSeasonNumber(
                    user, serie, Notification.Type.SERIES_SEASON, temporada)) {
                notificationRepository.save(Notification.novaTemporada(user, serie, temporada, detalhe));
                criados++;
            }
        }
        return criados;
    }

    /** quem tem a série na watchlist e quem já registrou a série ou uma temporada dela */
    private List<User> interessados(Series serie) {
        Map<Long, User> porId = new LinkedHashMap<>();
        for (SeriesWatchlistItem item : seriesWatchlistRepository.findBySeries(serie)) {
            porId.putIfAbsent(item.getUser().getId(), item.getUser());
        }
        for (User user : seriesReviewRepository.findUsuariosQueRegistraram(serie)) {
            porId.putIfAbsent(user.getId(), user);
        }
        return List.copyOf(porId.values());
    }
}
