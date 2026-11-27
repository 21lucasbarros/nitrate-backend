package com.lucasbarros.nitrate.services;

import java.util.Map;

/**
 * Teto dos caches em memória das respostas do TMDB. As chaves vêm de ids pedidos por qualquer
 * pessoa, então sem teto o cache cresceria até acabar a memória; passando do teto, o cache
 * recomeça vazio (o pior caso é buscar de novo no TMDB).
 */
final class CacheLimitado {

    static final int MAX_ENTRADAS = 5_000;

    private CacheLimitado() {
    }

    static <K, V> void guardar(Map<K, V> cache, K chave, V valor) {
        if (cache.size() >= MAX_ENTRADAS && !cache.containsKey(chave)) cache.clear();
        cache.put(chave, valor);
    }
}
