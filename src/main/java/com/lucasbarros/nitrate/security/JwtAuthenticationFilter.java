package com.lucasbarros.nitrate.security;

import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.RevokedTokenRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JwtService jwtService;

    @Autowired
    private RevokedTokenRepository revokedTokenRepository;

    @Autowired
    private UserRepository userRepository;

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private static final Duration INTERVALO_VISTO = Duration.ofMinutes(5);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        JwtService.Sessao sessao;
        try {
            sessao = jwtService.ler(token);
        } catch (JwtException | IllegalArgumentException e) {
            // token expirado/inválido: segue como anônimo. rotas públicas continuam
            // funcionando e as protegidas respondem 401 pelo entry point
            filterChain.doFilter(request, response);
            return;
        }

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            // conta excluída, sessões encerradas depois do token ou logout: segue como anônimo
            User user = userRepository.findById(sessao.userId()).orElse(null);
            if (user != null && jwtService.valeParaConta(sessao, user) && !revokedTokenRepository.existsById(sessao.id())) {
                CustomUserDetails userDetails = new CustomUserDetails(user);
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                marcarVisto(user);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * "Visto por último" para o painel de admin. Grava no máximo uma vez a cada INTERVALO_VISTO,
     * e não a cada requisição; uma falha aqui nunca derruba a requisição.
     */
    private void marcarVisto(User user) {
        Instant agora = Instant.now();
        Instant ultimo = user.getLastSeenAt();
        if (ultimo != null && ultimo.isAfter(agora.minus(INTERVALO_VISTO))) return;
        try {
            userRepository.marcarVisto(user.getId(), agora);
        } catch (RuntimeException e) {
            log.warn("não foi possível gravar o último acesso de {}: {}", user.getId(), e.getMessage());
        }
    }
}
