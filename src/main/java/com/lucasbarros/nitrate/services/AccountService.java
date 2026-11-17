package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.AccountExportDTO;
import com.lucasbarros.nitrate.dto.ProfileUpdateDTO;
import com.lucasbarros.nitrate.dto.UserProfileDTO;
import com.lucasbarros.nitrate.entities.Follow;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.FavoriteFilmRepository;
import com.lucasbarros.nitrate.repositories.FollowRepository;
import com.lucasbarros.nitrate.repositories.MovieListRepository;
import com.lucasbarros.nitrate.repositories.NotificationRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.repositories.WatchlistRepository;
import com.lucasbarros.nitrate.services.exceptions.ForbiddenException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

/** A conta da própria pessoa: editar o perfil, exportar os dados e excluir a conta (LGPD). */
@Service
public class AccountService {

    private static final int NOME_MAX = 60;
    private static final int BIO_MAX = 300;
    // regras dos próprios sites para nomes de usuário
    private static final Pattern TWITTER = Pattern.compile("^[A-Za-z0-9_]{1,15}$");
    private static final Pattern INSTAGRAM = Pattern.compile("^[A-Za-z0-9._]{1,30}$");
    // aceita colar o link do perfil em vez do @
    private static final Pattern PREFIXO_TWITTER = Pattern.compile("^(https?://)?(www\\.)?(twitter|x)\\.com/", Pattern.CASE_INSENSITIVE);
    private static final Pattern PREFIXO_INSTAGRAM = Pattern.compile("^(https?://)?(www\\.)?instagram\\.com/", Pattern.CASE_INSENSITIVE);

    @Autowired private UserRepository userRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private WatchlistRepository watchlistRepository;
    @Autowired private FavoriteFilmRepository favoriteFilmRepository;
    @Autowired private FollowRepository followRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private MovieListRepository movieListRepository;
    @Autowired private MovieListService movieListService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private UserService userService;

    @Transactional
    public UserProfileDTO atualizarPerfil(User user, ProfileUpdateDTO dados) {
        String nome = dados.name() == null ? "" : dados.name().trim();
        if (nome.isEmpty()) throw new IllegalArgumentException("O nome não pode ficar vazio.");
        if (nome.length() > NOME_MAX) throw new IllegalArgumentException("O nome pode ter até " + NOME_MAX + " caracteres.");

        String bio = vazioParaNulo(dados.bio());
        if (bio != null && bio.length() > BIO_MAX) {
            throw new IllegalArgumentException("A bio pode ter até " + BIO_MAX + " caracteres.");
        }

        String twitter = arroba(dados.twitter(), PREFIXO_TWITTER);
        if (twitter != null && !TWITTER.matcher(twitter).matches()) {
            throw new IllegalArgumentException("Usuário do X/Twitter inválido: use até 15 letras, números ou _.");
        }
        String instagram = arroba(dados.instagram(), PREFIXO_INSTAGRAM);
        if (instagram != null && !INSTAGRAM.matcher(instagram).matches()) {
            throw new IllegalArgumentException("Usuário do Instagram inválido: use até 30 letras, números, . ou _.");
        }

        // o principal do token é uma cópia desanexada: atualiza a linha do banco
        User salvo = userRepository.findById(user.getId()).orElseThrow();
        salvo.setName(nome);
        salvo.setBio(bio);
        salvo.setTwitter(twitter);
        salvo.setInstagram(instagram);
        return userService.perfil(userRepository.save(salvo));
    }

    /**
     * Exclusão definitiva (LGPD, art. 18, VI): apaga a conta e tudo ligado a ela — diário,
     * reviews, watchlist, favoritos, listas, quem segue e quem é seguido, notificações. Não há como desfazer.
     */
    @Transactional
    public void excluirConta(User user, String senha) {
        User salvo = userRepository.findById(user.getId()).orElseThrow();
        if (senha == null || !passwordEncoder.matches(senha, salvo.getPasswordHash())) {
            throw new ForbiddenException("Senha incorreta.");
        }
        notificationRepository.deleteAllByUser(salvo);
        movieListService.excluirTodas(salvo);
        favoriteFilmRepository.deleteAllByUser(salvo);
        watchlistRepository.deleteAllByUser(salvo);
        reviewRepository.deleteAllByUser(salvo);
        followRepository.deleteAllInvolving(salvo);
        userRepository.delete(salvo);
    }

    @Transactional(readOnly = true)
    public AccountExportDTO exportar(User user) {
        User u = userRepository.findById(user.getId()).orElseThrow();
        return new AccountExportDTO(
                Instant.now(),
                new AccountExportDTO.Profile(u.getName(), u.getUsername(), u.getEmail(), u.getBio(),
                        u.getTwitter(), u.getInstagram(), u.getCreatedAt()),
                reviewRepository.findByUserOrderByCreatedAtDesc(u).stream()
                        .map(r -> new AccountExportDTO.DiaryEntry(filme(r.getMovie()), r.getWatchedOn(), r.getRating(),
                                r.isLiked(), r.isRewatch(), r.getText(), r.isSpoiler(), r.getCreatedAt()))
                        .toList(),
                watchlistRepository.findByUserOrderByAddedAtDesc(u).stream()
                        .map(w -> new AccountExportDTO.WatchlistEntry(filme(w.getMovie()), w.getAddedAt()))
                        .toList(),
                favoriteFilmRepository.findByUserOrderByPositionAsc(u).stream()
                        .map(f -> filme(f.getMovie()))
                        .toList(),
                movieListRepository.findByUserOrderByUpdatedAtDesc(u).stream()
                        .map(l -> new AccountExportDTO.FilmList(l.getName(), l.getDescription(), l.isPublicList(),
                                l.getEntries().stream().map(e -> filme(e.getMovie())).toList(), l.getCreatedAt()))
                        .toList(),
                usernames(followRepository.findByFollowerOrderByCreatedAtDesc(u), true),
                usernames(followRepository.findByFollowedOrderByCreatedAtDesc(u), false));
    }

    private static AccountExportDTO.Film filme(Movie m) {
        return new AccountExportDTO.Film(m.getTitle(), m.getReleaseYear(), m.getTmdbId());
    }

    private static List<String> usernames(List<Follow> follows, boolean seguidos) {
        return follows.stream()
                .map(f -> (seguidos ? f.getFollowed() : f.getFollower()).getUsername())
                .toList();
    }

    private static String vazioParaNulo(String valor) {
        if (valor == null) return null;
        String limpo = valor.trim();
        return limpo.isEmpty() ? null : limpo;
    }

    /** "@fulano", "fulano" ou o link do perfil viram "fulano" */
    private static String arroba(String valor, Pattern prefixo) {
        String limpo = vazioParaNulo(valor);
        if (limpo == null) return null;
        limpo = prefixo.matcher(limpo).replaceFirst("");
        limpo = limpo.replaceFirst("^@", "").replaceFirst("[/?#].*$", "");
        return limpo.isEmpty() ? null : limpo;
    }
}
