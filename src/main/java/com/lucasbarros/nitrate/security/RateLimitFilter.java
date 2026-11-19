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

/**
 * Limite por IP nas rotas públicas que dá para abusar: login e cadastro (força bruta e contas
 * em massa) e as que chamam o TMDB sem login (gastam a cota da chave e importam filmes no banco).
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

    private final RateLimiter rateLimiter;

    public RateLimitFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        Regra regra = regraPara(request);
        if (regra != null && !rateLimiter.tentar(regra.nome() + ":" + ip(request), regra.limite(), regra.janela())) {
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
        // busca e importação sob demanda no TMDB (o resto de /movies e /people lê só o banco)
        if ("GET".equals(metodo) && (caminho.equals("/movies/search") || caminho.startsWith("/movies/tmdb/")
                || caminho.startsWith("/people/"))) {
            return TMDB;
        }
        return null;
    }

    /**
     * O host fica atrás de um proxy, então o IP real vem no X-Forwarded-For (o primeiro da lista).
     * Dá para forjar esse header e fugir do limite por IP; por isso o login também tem limite por
     * conta, que não depende dele.
     */
    private static String ip(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            return encaminhado.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
