package com.lucasbarros.nitrate.taste;

import com.lucasbarros.nitrate.entities.Movie;

/** se o filme tem uma palavra-chave do TMDB (ex.: "musical", que o TMDB não trata como gênero) */
public interface PalavrasChave {

    /** id da palavra-chave "musical" no TMDB */
    int MUSICAL = 4344;

    boolean tem(Movie filme, int palavraChave);

    /** a lista da palavra-chave está disponível; vazia (TMDB fora do ar), o filtro usa o plano B */
    boolean disponivel(int palavraChave);

    /** sem dados de palavras-chave: cada filtro usa o próprio plano B */
    PalavrasChave NENHUMA = new PalavrasChave() {
        public boolean tem(Movie filme, int palavraChave) { return false; }
        public boolean disponivel(int palavraChave) { return false; }
    };
}
