package com.lucasbarros.nitrate.security;

import jakarta.servlet.http.HttpServletRequest;

/**
 * O IP de quem fez a requisição, para os limites de tentativas. A API fica atrás do proxy do host,
 * que acrescenta o IP de quem se conectou a ele no fim do X-Forwarded-For. O começo da lista vem
 * de quem fez a requisição e pode ser inventado (mandar um IP diferente a cada tentativa fugiria
 * dos limites), então vale o último.
 */
public final class ClientIp {

    private ClientIp() {
    }

    public static String de(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            String[] ips = encaminhado.split(",");
            String ultimo = ips[ips.length - 1].trim();
            if (!ultimo.isEmpty()) return ultimo;
        }
        return request.getRemoteAddr();
    }
}
