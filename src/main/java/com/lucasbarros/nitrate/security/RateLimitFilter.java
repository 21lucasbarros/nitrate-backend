package com.lucasbarros.nitrate.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Pattern;

/**
 * Limite por IP nas rotas públicas que dá para abusar: login e cadastro (força bruta e contas
 * em massa), redefinição de senha (e-mails em massa e tokens tentados às cegas) e as que chamam o TMDB sem login (gastam a cota da chave e importam filmes e séries no banco).
 * O IP vem de ClientIp.
 * O limite por conta no login, que não depende do IP, fica no AuthService.
 *
 * Não é @Component de propósito: o Spring Boot registraria o filtro também fora da cadeia de
 * segurança, antes do CORS, e o 429 chegaria ao navegador como erro de CORS. O SecurityConfig
 * cria e posiciona o filtro logo depois do CORS.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private record Regra(String nome, int limite, Duration janela) {}

    private static final Regra LOGIN = new Regra("login", 20, Duration.ofMinutes(1));
    private static final Regra CADASTRO = new Regra("cadastro", 5, Duration.ofHours(1));
    private static final Regra TMDB = new Regra("tmdb", 60, Duration.ofMinutes(1));
    // detalhes, temporadas, pôsteres etc. de uma página já aberta: várias chamadas por página, limite mais folgado
    private static final Regra TMDB_DETALHES = new Regra("tmdb-detalhes", 180, Duration.ofMinutes(1));
    // cada pedido pode disparar um e-mail; tentar tokens às cegas também esbarra aqui
    private static final Regra ESQUECI_SENHA = new Regra("esqueci-senha", 5, Duration.ofHours(1));
    private static final Regra REDEFINIR_SENHA = new Regra("redefinir-senha", 10, Duration.ofMinutes(15));

    // rotas públicas de um filme ou de uma série que consultam o TMDB (com cache, mas um id novo é uma chamada)
    private static final Pattern DETALHES_TMDB = Pattern.compile(
            "^/movies/\\d+/(posters|credits|watch-providers)$"
            + "|^/series/\\d+/(details|seasons/\\d+|backdrops|posters|watch-providers)$"
            + "|^/series/(trending|top-rated)$");

    private final RateLimiter rateLimiter;

    public RateLimitFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        Regra regra = regraPara(request);
        if (regra != null && !rateLimiter.tentar(regra.nome() + ":" + ClientIp.de(request), regra.limite(), regra.janela())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", String.valueOf(regra.janela().toSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"timestamp\":\"" + Instant.now() + "\",\"status\":429,"
                    + "\"error\":\"Muitas tentativas\",\"message\":\"Muitas tentativas em pouco tempo. Espere um pouco e tente de novo.\","
                    + "\"path\":\"" + request.getRequestURI().replace("\"", "") + "\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static Regra regraPara(HttpServletRequest request) {
        String metodo = request.getMethod();
        String caminho = request.getRequestURI();
        if ("POST".equals(metodo) && caminho.equals("/auth/login")) return LOGIN;
        if ("POST".equals(metodo) && caminho.equals("/auth/register")) return CADASTRO;
        if ("POST".equals(metodo) && caminho.equals("/auth/forgot-password")) return ESQUECI_SENHA;
        if ("POST".equals(metodo) && caminho.equals("/auth/reset-password")) return REDEFINIR_SENHA;
        if (!"GET".equals(metodo)) return null;
        // busca e importação sob demanda no TMDB (o resto de /movies e /series lê só o banco)
        if (caminho.equals("/movies/search") || caminho.startsWith("/movies/tmdb/") || caminho.startsWith("/people/")
                || caminho.equals("/series/search") || caminho.startsWith("/series/tmdb/")) {
            return TMDB;
        }
        if (DETALHES_TMDB.matcher(caminho).matches()) return TMDB_DETALHES;
        return null;
    }

}
