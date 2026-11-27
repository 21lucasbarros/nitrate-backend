package com.lucasbarros.nitrate.security;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Contador de tentativas por chave em janelas fixas ("login:ana" -> 3 nesta janela de 15 min).
 * Fica em memória: a API roda numa instância só, e reiniciar zera os contadores (aceitável
 * para frear força bruta e abuso; não é cota de cobrança).
 */
@Component
public class RateLimiter {

    // acima disso, as janelas vencidas são descartadas antes de criar uma nova
    private static final int CHAVES_ANTES_DE_LIMPAR = 10_000;
    // teto da memória: se nem descartando as vencidas o mapa voltar para baixo disso, saem as janelas mais antigas
    private static final int CHAVES_MAX = 100_000;

    private record Janela(Instant inicio, Duration duracao, int contagem) {
        boolean vencida(Instant agora) {
            return !agora.isBefore(inicio.plus(duracao));
        }
    }

    private final Map<String, Janela> janelas = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimiter() {
        this(Clock.systemUTC());
    }

    RateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** conta uma tentativa; false se a chave já passou do limite nesta janela */
    public boolean tentar(String chave, int limite, Duration janela) {
        return registrar(chave, janela) <= limite;
    }

    /** só consulta, sem contar: a chave já atingiu o limite nesta janela? */
    public boolean esgotado(String chave, int limite) {
        Janela atual = janelas.get(chave);
        return atual != null && !atual.vencida(clock.instant()) && atual.contagem() >= limite;
    }

    /** conta uma ocorrência (ex.: senha errada) e devolve quantas já houve nesta janela */
    public int registrar(String chave, Duration janela) {
        Instant agora = clock.instant();
        if (janelas.size() > CHAVES_ANTES_DE_LIMPAR) {
            janelas.values().removeIf(j -> j.vencida(agora));
            if (janelas.size() > CHAVES_MAX) {
                janelas.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.comparing(Janela::inicio)))
                        .limit(janelas.size() - CHAVES_ANTES_DE_LIMPAR)
                        .map(Map.Entry::getKey)
                        .toList()
                        .forEach(janelas::remove);
            }
        }
        return janelas.compute(chave, (k, atual) -> atual == null || atual.vencida(agora)
                ? new Janela(agora, janela, 1)
                : new Janela(atual.inicio(), atual.duracao(), atual.contagem() + 1)).contagem();
    }

    public void zerar(String chave) {
        janelas.remove(chave);
    }

    /** zera todas as chaves que começam com `prefixo` (ex.: as falhas de login de uma conta, de todos os IPs) */
    public void zerarPrefixo(String prefixo) {
        janelas.keySet().removeIf(chave -> chave.startsWith(prefixo));
    }
}
