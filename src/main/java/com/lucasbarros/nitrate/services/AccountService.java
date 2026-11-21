package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.AccountExportDTO;
import com.lucasbarros.nitrate.dto.AuthResponseDTO;
import com.lucasbarros.nitrate.dto.EmailChangeDTO;
import com.lucasbarros.nitrate.dto.PasswordChangeDTO;
import com.lucasbarros.nitrate.dto.ProfileUpdateDTO;
import com.lucasbarros.nitrate.dto.UserProfileDTO;
import com.lucasbarros.nitrate.entities.FavoritePerson;
import com.lucasbarros.nitrate.entities.Follow;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.Series;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.FavoriteFilmRepository;
import com.lucasbarros.nitrate.repositories.FavoritePersonRepository;
import com.lucasbarros.nitrate.repositories.FollowRepository;
import com.lucasbarros.nitrate.repositories.MovieListRepository;
import com.lucasbarros.nitrate.repositories.NotificationRepository;
import com.lucasbarros.nitrate.repositories.PasswordResetTokenRepository;
import com.lucasbarros.nitrate.repositories.ReviewCommentRepository;
import com.lucasbarros.nitrate.repositories.ReviewLikeRepository;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.repositories.SeriesReviewCommentRepository;
import com.lucasbarros.nitrate.repositories.SeriesReviewLikeRepository;
import com.lucasbarros.nitrate.repositories.SeriesReviewRepository;
import com.lucasbarros.nitrate.repositories.SeriesWatchlistRepository;
import com.lucasbarros.nitrate.repositories.UserAvatarRepository;
import com.lucasbarros.nitrate.repositories.UserAchievementRepository;
import com.lucasbarros.nitrate.repositories.UserBadgeRepository;
import com.lucasbarros.nitrate.repositories.TasteProfileRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.repositories.WatchlistRepository;
import com.lucasbarros.nitrate.security.CustomUserDetails;
import com.lucasbarros.nitrate.security.JwtService;
import com.lucasbarros.nitrate.security.RateLimiter;
import com.lucasbarros.nitrate.services.exceptions.EmailJaCadastradoException;
import com.lucasbarros.nitrate.services.exceptions.ForbiddenException;
import com.lucasbarros.nitrate.services.exceptions.TooManyRequestsException;
import com.lucasbarros.nitrate.services.exceptions.UsernameJaCadastradoException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** A conta da própria pessoa: editar o perfil, exportar os dados e excluir a conta (LGPD). */
@Service
public class AccountService {

    private static final int NOME_MAX = 60;
    private static final int BIO_MAX = 300;
    // a senha atual é conferida a cada tentativa: limita quem tenta adivinhá-la com uma sessão aberta
    private static final int TROCAS_SENHA_MAX = 10;
    private static final Duration JANELA_TROCAS_SENHA = Duration.ofMinutes(15);
    // a troca de e-mail também confere a senha: mesmo limite
    private static final int TROCAS_EMAIL_MAX = 10;
    private static final Duration JANELA_TROCAS_EMAIL = Duration.ofMinutes(15);
    // regras dos próprios sites para nomes de usuário
    private static final Pattern TWITTER = Pattern.compile("^[A-Za-z0-9_]{1,15}$");
    private static final Pattern INSTAGRAM = Pattern.compile("^[A-Za-z0-9._]{1,30}$");
    // aceita colar o link do perfil em vez do @
    private static final Pattern PREFIXO_TWITTER = Pattern.compile("^(https?://)?(www\\.)?(twitter|x)\\.com/", Pattern.CASE_INSENSITIVE);
    private static final Pattern PREFIXO_INSTAGRAM = Pattern.compile("^(https?://)?(www\\.)?instagram\\.com/", Pattern.CASE_INSENSITIVE);

    @Autowired private UserRepository userRepository;
    @Autowired private UserAvatarRepository userAvatarRepository;
    @Autowired private UserBadgeRepository userBadgeRepository;
    @Autowired private UserAchievementRepository userAchievementRepository;
    @Autowired private TasteProfileRepository tasteProfileRepository;
    @Autowired private ReviewRepository reviewRepository;
    @Autowired private WatchlistRepository watchlistRepository;
    @Autowired private SeriesReviewRepository seriesReviewRepository;
    @Autowired private SeriesWatchlistRepository seriesWatchlistRepository;
    @Autowired private SeriesReviewLikeRepository seriesReviewLikeRepository;
    @Autowired private SeriesReviewCommentRepository seriesReviewCommentRepository;
    @Autowired private FavoriteFilmRepository favoriteFilmRepository;
    @Autowired private FavoritePersonRepository favoritePersonRepository;
    @Autowired private FollowRepository followRepository;
    @Autowired private NotificationRepository notificationRepository;
    @Autowired private MovieListRepository movieListRepository;
    @Autowired private ReviewLikeRepository reviewLikeRepository;
    @Autowired private ReviewCommentRepository reviewCommentRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private MovieListService movieListService;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtService jwtService;
    @Autowired private RateLimiter rateLimiter;
    @Autowired private EmailService emailService;
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

        // trocar o @: mesmas regras do cadastro. O token guarda o e-mail, então a sessão continua valendo
        if (dados.username() != null) {
            String username = dados.username().trim();
            if (!username.equals(salvo.getUsername())) {
                AuthService.validarUsername(username);
                // só mudar maiúsculas/minúsculas do próprio @ não esbarra nele mesmo
                boolean outroPerfil = !username.equalsIgnoreCase(salvo.getUsername());
                if (outroPerfil && (AuthService.usernameReservado(username) || userRepository.existsByUsernameIgnoreCase(username))) {
                    throw new UsernameJaCadastradoException(username);
                }
                salvo.setUsername(username);
            }
        }

        salvo.setName(nome);
        salvo.setBio(bio);
        salvo.setTwitter(twitter);
        salvo.setInstagram(instagram);
        return userService.perfil(userRepository.save(salvo));
    }

    /**
     * Troca a senha sabendo a atual. Derruba as sessões abertas nos outros aparelhos e devolve um
     * token novo para este continuar conectado.
     */
    @Transactional
    public AuthResponseDTO trocarSenha(User user, PasswordChangeDTO dados) {
        if (!rateLimiter.tentar("troca-senha:" + user.getId(), TROCAS_SENHA_MAX, JANELA_TROCAS_SENHA)) {
            throw new TooManyRequestsException("Muitas tentativas de trocar a senha. Espere alguns minutos e tente de novo.");
        }
        User salvo = userRepository.findById(user.getId()).orElseThrow();
        if (dados.currentPassword() == null || !passwordEncoder.matches(dados.currentPassword(), salvo.getPasswordHash())) {
            throw new ForbiddenException("Senha atual incorreta.");
        }
        AuthService.validarSenha(dados.newPassword());
        if (passwordEncoder.matches(dados.newPassword(), salvo.getPasswordHash())) {
            throw new IllegalArgumentException("A nova senha precisa ser diferente da atual.");
        }

        salvo.setPasswordHash(passwordEncoder.encode(dados.newPassword()));
        salvo.setPasswordChangedAt(Instant.now());
        userRepository.save(salvo);
        passwordResetTokenRepository.deleteAllByUser(salvo); // um link de redefinição pendente não vale mais

        emailService.enviarEmSegundoPlano(salvo.getEmail(), "Sua senha do Nitrate foi alterada",
                avisoSenhaAlteradaHtml(salvo.getName()), avisoSenhaAlteradaTexto(salvo.getName()));

        String token = jwtService.gerarToken(new CustomUserDetails(salvo));
        return new AuthResponseDTO(token, salvo.getName(), salvo.getUsername(), salvo.getEmail());
    }

    /**
     * Troca o e-mail sabendo a senha. O token guarda o e-mail, então as sessões abertas nos outros
     * aparelhos caem sozinhas; devolve um token novo para este continuar conectado. O e-mail antigo
     * recebe um aviso, a chance de recuperar a conta se não foi a própria pessoa.
     */
    @Transactional
    public AuthResponseDTO trocarEmail(User user, EmailChangeDTO dados) {
        if (!rateLimiter.tentar("troca-email:" + user.getId(), TROCAS_EMAIL_MAX, JANELA_TROCAS_EMAIL)) {
            throw new TooManyRequestsException("Muitas tentativas de trocar o e-mail. Espere alguns minutos e tente de novo.");
        }
        User salvo = userRepository.findById(user.getId()).orElseThrow();
        if (dados.password() == null || !passwordEncoder.matches(dados.password(), salvo.getPasswordHash())) {
            throw new ForbiddenException("Senha incorreta.");
        }
        String email = dados.email() == null ? "" : dados.email().trim().toLowerCase(Locale.ROOT);
        AuthService.validarEmail(email);
        if (email.equalsIgnoreCase(salvo.getEmail())) {
            throw new IllegalArgumentException("Esse já é o e-mail da sua conta.");
        }
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new EmailJaCadastradoException(email);
        }

        String antigo = salvo.getEmail();
        salvo.setEmail(email);
        userRepository.save(salvo);
        passwordResetTokenRepository.deleteAllByUser(salvo); // um link de redefinição enviado ao e-mail antigo não vale mais

        emailService.enviarEmSegundoPlano(antigo, "O e-mail da sua conta no Nitrate foi alterado",
                avisoEmailAlteradoHtml(salvo.getName(), email), avisoEmailAlteradoTexto(salvo.getName(), email));

        String token = jwtService.gerarToken(new CustomUserDetails(salvo));
        return new AuthResponseDTO(token, salvo.getName(), salvo.getUsername(), salvo.getEmail());
    }

    private static String avisoEmailAlteradoTexto(String nome, String novo) {
        return "Olá, " + nome + "!\n\n"
                + "O e-mail da sua conta no Nitrate acabou de ser trocado para " + novo + ", e as sessões abertas em outros aparelhos foram encerradas.\n\n"
                + "Se não foi você, fale com a gente o quanto antes para recuperarmos a sua conta.";
    }

    private static String avisoEmailAlteradoHtml(String nome, String novo) {
        return """
                <div style="font-family: -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px 24px; color: #1a1b1e;">
                  <p style="font-size: 20px; font-weight: 700; letter-spacing: -0.5px; text-transform: uppercase; margin: 0 0 24px;">Nitrate<span style="color: #FF7A33;">.</span></p>
                  <p style="font-size: 16px; line-height: 1.6;">Olá, %s!</p>
                  <p style="font-size: 16px; line-height: 1.6;">O e-mail da sua conta acabou de ser trocado para <strong>%s</strong>, e as sessões abertas em outros aparelhos foram encerradas.</p>
                  <p style="font-size: 14px; line-height: 1.6; color: #6b6f76;">Se não foi você, fale com a gente o quanto antes para recuperarmos a sua conta.</p>
                </div>
                """.formatted(HtmlUtils.htmlEscape(nome), HtmlUtils.htmlEscape(novo));
    }

    // se não foi a própria pessoa, o aviso é a chance de ela perceber e recuperar a conta
    private static String avisoSenhaAlteradaTexto(String nome) {
        return "Olá, " + nome + "!\n\n"
                + "A senha da sua conta no Nitrate acabou de ser alterada, e as sessões abertas em outros aparelhos foram encerradas.\n\n"
                + "Se não foi você, use \"Esqueci minha senha\" na tela de login para criar uma nova senha agora.";
    }

    private static String avisoSenhaAlteradaHtml(String nome) {
        return """
                <div style="font-family: -apple-system, Segoe UI, Roboto, Helvetica, Arial, sans-serif; max-width: 480px; margin: 0 auto; padding: 32px 24px; color: #1a1b1e;">
                  <p style="font-size: 20px; font-weight: 700; letter-spacing: -0.5px; text-transform: uppercase; margin: 0 0 24px;">Nitrate<span style="color: #FF7A33;">.</span></p>
                  <p style="font-size: 16px; line-height: 1.6;">Olá, %s!</p>
                  <p style="font-size: 16px; line-height: 1.6;">A senha da sua conta acabou de ser alterada, e as sessões abertas em outros aparelhos foram encerradas.</p>
                  <p style="font-size: 14px; line-height: 1.6; color: #6b6f76;">Se não foi você, use <strong>Esqueci minha senha</strong> na tela de login para criar uma nova senha agora.</p>
                </div>
                """.formatted(HtmlUtils.htmlEscape(nome));
    }

    /**
     * Exclusão definitiva (LGPD, art. 18, VI): apaga a conta e tudo ligado a ela — diário,
     * reviews, curtidas e comentários, watchlist, favoritos, listas, quem segue e quem é seguido, notificações, pedidos de redefinição de senha e a foto de perfil. Não há como desfazer.
     */
    @Transactional
    public void excluirConta(User user, String senha) {
        User salvo = userRepository.findById(user.getId()).orElseThrow();
        if (senha == null || !passwordEncoder.matches(senha, salvo.getPasswordHash())) {
            throw new ForbiddenException("Senha incorreta.");
        }
        notificationRepository.deleteAllByUser(salvo);
        passwordResetTokenRepository.deleteAllByUser(salvo);
        movieListService.excluirTodas(salvo);
        // antes das reviews: curtidas e comentários apontam para elas
        reviewLikeRepository.deleteAllInvolving(salvo);
        reviewCommentRepository.deleteAllInvolving(salvo);
        favoriteFilmRepository.deleteAllByUser(salvo);
        favoritePersonRepository.deleteAllByUser(salvo);
        watchlistRepository.deleteAllByUser(salvo);
        reviewRepository.deleteAllByUser(salvo);
        seriesWatchlistRepository.deleteAllByUser(salvo);
        // antes dos registros de séries: curtidas e comentários apontam para eles
        seriesReviewLikeRepository.deleteAllInvolving(salvo);
        seriesReviewCommentRepository.deleteAllInvolving(salvo);
        seriesReviewRepository.deleteAllByUser(salvo);
        followRepository.deleteAllInvolving(salvo);
        userBadgeRepository.deleteAllByUser(salvo);
        userAchievementRepository.deleteAllByUser(salvo);
        tasteProfileRepository.findById(salvo.getId()).ifPresent(tasteProfileRepository::delete);
        userAvatarRepository.findById(salvo.getId()).ifPresent(userAvatarRepository::delete);
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
                favoritePersonRepository.findByUserOrderByPositionAsc(u).stream()
                        .map(f -> new AccountExportDTO.FavoritePerson(
                                f.getKind() == FavoritePerson.Kind.ACTOR ? "actor" : "director", f.getName(), f.getTmdbId()))
                        .toList(),
                movieListRepository.findByUserOrderByUpdatedAtDesc(u).stream()
                        .map(l -> new AccountExportDTO.FilmList(l.getName(), l.getDescription(), l.isPublicList(),
                                l.getEntries().stream().map(e -> filme(e.getMovie())).toList(), l.getCreatedAt()))
                        .toList(),
                reviewCommentRepository.findByUserOrderByCreatedAtDesc(u).stream()
                        .map(c -> new AccountExportDTO.Comment(filme(c.getReview().getMovie()),
                                c.getReview().getUser().getUsername(), c.getText(), c.getCreatedAt()))
                        .toList(),
                reviewLikeRepository.findByUserOrderByCreatedAtDesc(u).stream()
                        .map(l -> new AccountExportDTO.ReviewLike(filme(l.getReview().getMovie()),
                                l.getReview().getUser().getUsername(), l.getCreatedAt()))
                        .toList(),
                usernames(followRepository.findByFollowerOrderByCreatedAtDesc(u), true),
                usernames(followRepository.findByFollowedOrderByCreatedAtDesc(u), false),
                seriesReviewRepository.findByUserOrderByCreatedAtDesc(u).stream()
                        .map(r -> new AccountExportDTO.SeriesDiaryEntry(serie(r.getSeries()), r.getSeasonNumber(),
                                r.getWatchedOn(), r.getRating(), r.isLiked(), r.getText(), r.isSpoiler(), r.getCreatedAt()))
                        .toList(),
                seriesWatchlistRepository.findByUserOrderByAddedAtDesc(u).stream()
                        .map(w -> new AccountExportDTO.SeriesWatchlistEntry(serie(w.getSeries()), w.getAddedAt()))
                        .toList());
    }

    private static AccountExportDTO.Film filme(Movie m) {
        return new AccountExportDTO.Film(m.getTitle(), m.getReleaseYear(), m.getTmdbId());
    }

    private static AccountExportDTO.Show serie(Series s) {
        return new AccountExportDTO.Show(s.getName(), s.getFirstAirYear(), s.getTmdbId());
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
