package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.FilmDTO;
import com.lucasbarros.nitrate.dto.MovieUserStateDTO;
import com.lucasbarros.nitrate.dto.PageDTO;
import com.lucasbarros.nitrate.dto.ReviewDTO;
import com.lucasbarros.nitrate.dto.UserProfileDTO;
import com.lucasbarros.nitrate.dto.WatchlistItemDTO;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.entities.WatchlistItem;
import com.lucasbarros.nitrate.repositories.MovieRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.repositories.WatchlistRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class UserService {

    @Autowired private UserRepository userRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private WatchlistRepository watchlistRepository;
    @Autowired private MovieRepository movieRepository;
    @Autowired private SocialService socialService;

    /** `quemVe`: usuário logado (ou nulo), para dizer se ele segue este perfil e se é seguido por ele */
    @Transactional(readOnly = true)
    public UserProfileDTO perfil(String username, User quemVe) {
        User user = buscarPorUsername(username);
        boolean outraPessoa = quemVe != null && !quemVe.getId().equals(user.getId());
        Boolean segue = outraPessoa ? socialService.segue(quemVe, user) : null;
        Boolean meSegue = outraPessoa ? socialService.segue(user, quemVe) : null;
        return perfil(user, segue, meSegue);
    }

    @Transactional(readOnly = true)
    public UserProfileDTO perfil(User user) {
        return perfil(user, null, null);
    }

    private UserProfileDTO perfil(User user, Boolean seguidoPorQuemVe, Boolean segueQuemVe) {
        return new UserProfileDTO(
                user,
                reviewRepository.countFilmesDistintos(user),
                reviewRepository.countByUserAndTextIsNotNull(user),
                reviewRepository.countByUserAndLikedTrue(user),
                watchlistRepository.countByUser(user),
                socialService.contarSeguidores(user),
                socialService.contarSeguindo(user),
                socialService.favoritos(user),
                seguidoPorQuemVe,
                segueQuemVe);
    }

    private static final int TAMANHO_MAX_PAGINA = 60;

    /** diário paginado, do que assistiu por último para trás; `comTexto` = só reviews escritas */
    @Transactional(readOnly = true)
    public PageDTO<ReviewDTO> reviews(String username, int pagina, int tamanho, boolean comTexto) {
        User user = buscarPorUsername(username);
        PageRequest pageRequest = PageRequest.of(
                Math.max(0, pagina),
                Math.min(Math.max(1, tamanho), TAMANHO_MAX_PAGINA),
                Sort.by(Sort.Order.desc("watchedOn"), Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        Page<Review> resultado = comTexto
                ? reviewRepository.findByUserAndTextIsNotNull(user, pageRequest)
                : reviewRepository.findByUser(user, pageRequest);
        return PageDTO.of(resultado, ReviewDTO::new);
    }

    /** ordens da aba "Filmes" do perfil; o padrão é do lançamento mais novo para o mais antigo */
    public enum OrdemFilmes {
        LANCAMENTO, LANCAMENTO_ANTIGOS, NOTA, NOTA_MENOR, ASSISTIDOS, TITULO;

        /** "nota-menor" -> NOTA_MENOR; valor desconhecido cai no padrão */
        public static OrdemFilmes de(String valor) {
            if (valor == null) return LANCAMENTO;
            try {
                return valueOf(valor.trim().toUpperCase(Locale.ROOT).replace('-', '_'));
            } catch (IllegalArgumentException e) {
                return LANCAMENTO;
            }
        }
    }

    // do registro mais recente para o mais antigo (mesma ordem do diário)
    private static final Comparator<Review> MAIS_RECENTE_PRIMEIRO =
            Comparator.comparing(Review::getWatchedOn, Comparator.nullsLast(Comparator.<LocalDate>reverseOrder()))
                    .thenComparing(Review::getCreatedAt, Comparator.reverseOrder())
                    .thenComparing(Review::getId, Comparator.reverseOrder());

    /**
     * Todos os filmes que a pessoa assistiu, cada um uma vez só (reassistir não repete o filme).
     * A nota, a curtida e o link vêm do registro mais recente; junto vão todas as vezes que ela
     * assistiu, para a tela deixar escolher qual review abrir.
     * A ordenação é feita em memória porque depende do filme (lançamento, título) e do registro
     * escolhido para ele; um diário tem no máximo alguns milhares de registros.
     */
    @Transactional(readOnly = true)
    public PageDTO<FilmDTO> filmes(String username, int pagina, int tamanho, String ordem, boolean soCurtidos) {
        User user = buscarPorUsername(username);
        List<Review> registros = new ArrayList<>(reviewRepository.findByUserOrderByCreatedAtDesc(user));
        registros.sort(MAIS_RECENTE_PRIMEIRO);

        // registros de cada filme, do mais recente para o mais antigo (a lista já vem nessa ordem)
        Map<Long, List<Review>> porFilme = new LinkedHashMap<>();
        for (Review review : registros) {
            porFilme.computeIfAbsent(review.getMovie().getId(), id -> new ArrayList<>()).add(review);
        }
        List<Review> filmes = new ArrayList<>();
        for (List<Review> doFilme : porFilme.values()) {
            filmes.add(doFilme.get(0));
        }
        if (soCurtidos) {
            filmes.removeIf(review -> !review.isLiked());
        }
        filmes.sort(comparadorDeFilmes(OrdemFilmes.de(ordem)));

        int tamanhoPagina = Math.min(Math.max(1, tamanho), TAMANHO_MAX_PAGINA);
        int totalPaginas = (int) Math.ceil(filmes.size() / (double) tamanhoPagina);
        int paginaAtual = Math.max(0, pagina);
        int inicio = Math.min(paginaAtual * tamanhoPagina, filmes.size());
        int fim = Math.min(inicio + tamanhoPagina, filmes.size());
        List<FilmDTO> itens = filmes.subList(inicio, fim).stream()
                .map(maisRecente -> new FilmDTO(porFilme.get(maisRecente.getMovie().getId())))
                .toList();
        return new PageDTO<>(itens, paginaAtual, tamanhoPagina, filmes.size(), totalPaginas);
    }

    private static Comparator<Review> comparadorDeFilmes(OrdemFilmes ordem) {
        // filme sem data de estreia nem ano vai para o fim, nas duas direções
        Comparator<Review> lancamentoNovos = Comparator.comparing(UserService::lancamento,
                Comparator.nullsLast(Comparator.<LocalDate>reverseOrder()));
        Comparator<Review> lancamentoAntigos = Comparator.comparing(UserService::lancamento,
                Comparator.nullsLast(Comparator.<LocalDate>naturalOrder()));
        Comparator<Review> titulo = Comparator.comparing(r -> r.getMovie().getTitle(), String.CASE_INSENSITIVE_ORDER);

        return switch (ordem) {
            case LANCAMENTO -> lancamentoNovos.thenComparing(titulo);
            case LANCAMENTO_ANTIGOS -> lancamentoAntigos.thenComparing(titulo);
            // sem nota vai para o fim; empates ficam do lançamento mais novo para o mais antigo
            case NOTA -> Comparator.comparing(Review::getRating, Comparator.nullsLast(Comparator.<Double>reverseOrder()))
                    .thenComparing(lancamentoNovos).thenComparing(titulo);
            case NOTA_MENOR -> Comparator.comparing(Review::getRating, Comparator.nullsLast(Comparator.<Double>naturalOrder()))
                    .thenComparing(lancamentoNovos).thenComparing(titulo);
            case ASSISTIDOS -> MAIS_RECENTE_PRIMEIRO;
            case TITULO -> titulo.thenComparing(lancamentoNovos);
        };
    }

    /** data de estreia; sem ela, 1º de janeiro do ano; sem nenhum dos dois, null */
    private static LocalDate lancamento(Review review) {
        Movie filme = review.getMovie();
        if (filme.getReleaseDate() != null) return filme.getReleaseDate();
        if (filme.getReleaseYear() != null) return LocalDate.of(filme.getReleaseYear(), 1, 1);
        return null;
    }

    @Transactional(readOnly = true)
    public List<WatchlistItemDTO> watchlist(String username) {
        User user = buscarPorUsername(username);
        List<WatchlistItemDTO> dtos = new ArrayList<>();
        for (WatchlistItem item : watchlistRepository.findByUserOrderByAddedAtDesc(user)) {
            dtos.add(new WatchlistItemDTO(item));
        }
        return dtos;
    }

    @Transactional(readOnly = true)
    public MovieUserStateDTO estadoDoFilme(User user, Long movieId) {
        Movie filme = movieRepository.findById(movieId)
                .orElseThrow(() -> new ResourceNotFoundException("Filme não encontrado. Id: " + movieId));
        ReviewDTO review = reviewRepository.findFirstByUserAndMovieOrderByWatchedOnDescCreatedAtDesc(user, filme)
                .map(ReviewDTO::new)
                .orElse(null);
        return new MovieUserStateDTO(review, reviewRepository.countByUserAndMovie(user, filme),
                watchlistRepository.existsByUserAndMovie(user, filme));
    }

    private User buscarPorUsername(String username) {
        return userRepository.findByUsername(username.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + username));
    }
}
