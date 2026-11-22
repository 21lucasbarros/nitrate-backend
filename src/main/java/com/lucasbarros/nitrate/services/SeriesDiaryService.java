package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.SeasonDTO;
import com.lucasbarros.nitrate.dto.SeriesDTO;
import com.lucasbarros.nitrate.dto.SeriesReviewDTO;
import com.lucasbarros.nitrate.dto.SeriesReviewRequestDTO;
import com.lucasbarros.nitrate.dto.SeriesUserStateDTO;
import com.lucasbarros.nitrate.entities.Series;
import com.lucasbarros.nitrate.entities.SeriesReview;
import com.lucasbarros.nitrate.entities.SeriesWatchlistItem;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.SeriesReviewRepository;
import com.lucasbarros.nitrate.repositories.SeriesWatchlistRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.services.exceptions.ForbiddenException;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import com.lucasbarros.nitrate.tmdb.TmdbImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** O diário de séries: registros (da série inteira, de uma temporada ou de um episódio) e a watchlist de séries. */
@Service
public class SeriesDiaryService {

    private static final int TEXTO_MAX = 20000;
    private static final int FEED_MAX = 30;
    private static final int RECENTES_MAX = 20;

    @Autowired private SeriesReviewRepository seriesReviewRepository;
    @Autowired private SeriesWatchlistRepository seriesWatchlistRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SeriesService seriesService;
    @Autowired private SeriesReviewSocialService seriesReviewSocialService;

    @Transactional
    public SeriesReviewDTO criar(User user, SeriesReviewRequestDTO request) {
        if (request.seriesId() == null) {
            throw new IllegalArgumentException("Informe a série (seriesId)");
        }
        Series serie = seriesService.buscar(request.seriesId());
        SeriesReview review = new SeriesReview(user, serie);
        aplicar(review, request);
        review = seriesReviewRepository.save(review);

        // terminou a série inteira: sai da watchlist
        if (review.getSeasonNumber() == null) {
            seriesWatchlistRepository.findByUserAndSeries(user, serie).ifPresent(seriesWatchlistRepository::delete);
        }
        return new SeriesReviewDTO(review);
    }

    @Transactional
    public SeriesReviewDTO atualizar(User user, Long id, SeriesReviewRequestDTO request) {
        SeriesReview review = buscarDoUsuario(user, id);
        aplicar(review, request);
        review.setUpdatedAt(Instant.now());
        return new SeriesReviewDTO(review);
    }

    @Transactional
    public void deletar(User user, Long id) {
        SeriesReview review = buscarDoUsuario(user, id);
        seriesReviewSocialService.limparReview(review);
        seriesReviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public SeriesReviewDTO buscarPorId(Long id) {
        return seriesReviewRepository.findById(id)
                .map(SeriesReviewDTO::new)
                .orElseThrow(() -> new ResourceNotFoundException("Review não encontrada. Id: " + id));
    }

    @Transactional(readOnly = true)
    public List<SeriesReviewDTO> daSerie(Long seriesId) {
        return seriesReviewRepository.findBySeriesOrderByCreatedAtDesc(seriesService.buscar(seriesId)).stream()
                .map(SeriesReviewDTO::new)
                .toList();
    }

    /** o que as pessoas que eu sigo registraram de séries por último */
    @Transactional(readOnly = true)
    public List<SeriesReviewDTO> feed(User eu, int limite) {
        int tamanho = Math.min(Math.max(1, limite), FEED_MAX);
        return seriesReviewRepository.findFeed(eu, PageRequest.of(0, tamanho)).stream()
                .map(SeriesReviewDTO::new)
                .toList();
    }

    /** os registros de séries mais recentes de todo mundo (a home de quem ainda não segue ninguém) */
    @Transactional(readOnly = true)
    public List<SeriesReviewDTO> recentes(int limite) {
        int tamanho = Math.min(Math.max(1, limite), RECENTES_MAX);
        return seriesReviewRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, tamanho)).stream()
                .map(SeriesReviewDTO::new)
                .toList();
    }

    /** todos os registros de séries da pessoa, do mais recente para o mais antigo */
    @Transactional(readOnly = true)
    public List<SeriesReviewDTO> doUsuario(String username) {
        return seriesReviewRepository.findByUserOrderByCreatedAtDesc(buscarPorUsername(username)).stream()
                .map(SeriesReviewDTO::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SeriesUserStateDTO.WatchlistItem> watchlistDoUsuario(String username) {
        return seriesWatchlistRepository.findByUserOrderByAddedAtDesc(buscarPorUsername(username)).stream()
                .map(w -> new SeriesUserStateDTO.WatchlistItem(new SeriesDTO(w.getSeries()), w.getAddedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public SeriesUserStateDTO estado(User user, Long seriesId) {
        Series serie = seriesService.buscar(seriesId);
        List<SeriesReviewDTO> reviews = seriesReviewRepository
                .findByUserAndSeriesOrderByWatchedOnDescCreatedAtDesc(user, serie).stream()
                .map(SeriesReviewDTO::new)
                .toList();
        return new SeriesUserStateDTO(reviews, seriesWatchlistRepository.existsByUserAndSeries(user, serie));
    }

    // idempotente: adicionar o que já está na lista não muda nada
    @Transactional
    public void adicionarNaWatchlist(User user, Long seriesId) {
        Series serie = seriesService.buscar(seriesId);
        if (!seriesWatchlistRepository.existsByUserAndSeries(user, serie)) {
            seriesWatchlistRepository.save(new SeriesWatchlistItem(user, serie));
        }
    }

    @Transactional
    public void removerDaWatchlist(User user, Long seriesId) {
        Series serie = seriesService.buscar(seriesId);
        seriesWatchlistRepository.findByUserAndSeries(user, serie).ifPresent(seriesWatchlistRepository::delete);
    }

    private SeriesReview buscarDoUsuario(User user, Long id) {
        SeriesReview review = seriesReviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review não encontrada. Id: " + id));
        if (!review.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Você só pode alterar as suas próprias reviews.");
        }
        return review;
    }

    /**
     * A série, a temporada ou o episódio já estreou. Temporada e episódio usam a data do TMDB (a mesma
     * consulta da página, em cache); sem a data, ou com o TMDB fora do ar, fica no benefício da dúvida.
     */
    private boolean jaFoiAoAr(Series serie, Integer temporada, Integer episodio) {
        if (!serie.jaEstreou()) return false;
        if (temporada == null) return true;
        try {
            SeasonDTO t = seriesService.temporada(serie.getId(), temporada);
            String data = episodio == null ? t.airDate() : t.episodes().stream()
                    .filter(e -> e.episodeNumber() == episodio)
                    .map(SeasonDTO.Episode::airDate)
                    .findFirst().orElse(null);
            LocalDate estreia = TmdbImportService.extrairData(data);
            return estreia == null || !estreia.isAfter(LocalDate.now());
        } catch (RuntimeException e) {
            return true;
        }
    }

    private void aplicar(SeriesReview review, SeriesReviewRequestDTO request) {
        Double nota = request.rating();
        if (nota != null && (nota < 0.5 || nota > 5 || nota * 2 != Math.floor(nota * 2))) {
            throw new IllegalArgumentException("A nota deve ir de 0.5 a 5, em passos de 0.5");
        }

        Integer temporada = request.seasonNumber();
        Integer totalTemporadas = review.getSeries().getNumberOfSeasons();
        if (temporada != null && (temporada < 0 || (totalTemporadas != null && temporada > totalTemporadas))) {
            throw new IllegalArgumentException("Temporada inválida: " + temporada);
        }

        Integer episodio = request.episodeNumber();
        if (episodio != null && temporada == null) {
            throw new IllegalArgumentException("Informe a temporada do episódio (seasonNumber)");
        }
        if (episodio != null && episodio < 1) {
            throw new IllegalArgumentException("Episódio inválido: " + episodio);
        }
        // como nos filmes: antes da estreia dá para registrar, escrever e curtir, mas não dar nota
        if (nota != null && !jaFoiAoAr(review.getSeries(), temporada, episodio)) {
            String oQue = episodio != null ? "Este episódio ainda não foi ao ar" : temporada != null
                    ? "Esta temporada ainda não estreou" : "Esta série ainda não estreou";
            throw new IllegalArgumentException(oQue + ": a nota fica liberada a partir da estreia");
        }

        String texto = request.text() == null ? null : request.text().trim();
        if (texto != null && texto.isEmpty()) texto = null;
        if (texto != null && texto.length() > TEXTO_MAX) {
            throw new IllegalArgumentException("A review pode ter no máximo " + TEXTO_MAX + " caracteres");
        }

        LocalDate assistidoEm = request.watchedOn() == null ? LocalDate.now() : request.watchedOn();
        if (assistidoEm.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("A data em que assistiu não pode estar no futuro");
        }

        review.setSeasonNumber(temporada);
        review.setEpisodeNumber(episodio);
        review.setRating(nota);
        review.setText(texto);
        review.setLiked(request.liked());
        // sem texto não há o que esconder
        review.setSpoiler(texto != null && request.spoiler());
        review.setWatchedOn(assistidoEm);
    }

    private User buscarPorUsername(String username) {
        return userRepository.findFirstByUsernameIgnoreCase(username.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + username));
    }
}
