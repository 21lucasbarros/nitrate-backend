package com.lucasbarros.nitrate.services;

import com.lucasbarros.nitrate.dto.MovieDTO;
import com.lucasbarros.nitrate.dto.RetrospectiveDTO;
import com.lucasbarros.nitrate.dto.RetrospectiveDTO.Film;
import com.lucasbarros.nitrate.dto.RetrospectiveDTO.RankedItem;
import com.lucasbarros.nitrate.entities.Genre;
import com.lucasbarros.nitrate.entities.Movie;
import com.lucasbarros.nitrate.entities.MovieCast;
import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.User;
import com.lucasbarros.nitrate.repositories.ReviewRepository;
import com.lucasbarros.nitrate.repositories.UserRepository;
import com.lucasbarros.nitrate.services.exceptions.ResourceNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class RetrospectiveService {

    private static final int TOP_DIRETORES = 5;
    private static final int TOP_GENEROS = 5;
    private static final int TOP_ATORES = 6;
    private static final int TOP_FAVORITOS = 5;
    private static final int MIN_FILMES_ATOR_RECORRENTE = 2;

    @Autowired private UserRepository userRepository;
    @Autowired private ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public RetrospectiveDTO retrospectiva(String username, String mes) {
        User user = buscarUsuario(username);
        YearMonth periodo = parseMes(mes);
        List<Review> reviews = reviewRepository.findByUserAndWatchedOnBetweenOrderByWatchedOnAsc(
                user, periodo.atDay(1), periodo.atEndOfMonth());

        Map<String, Integer> porDiretor = new HashMap<>();
        Map<String, Integer> porGenero = new HashMap<>();
        Map<String, Integer> porAtor = new HashMap<>();
        Map<String, Integer> distribuicao = new LinkedHashMap<>();
        for (double nota = 0.5; nota <= 5.0; nota += 0.5) {
            distribuicao.put(String.valueOf(nota), 0);
        }

        int comTexto = 0;
        int curtidos = 0;
        double somaNotas = 0;
        int comNota = 0;
        List<Film> filmes = new ArrayList<>();

        for (Review review : reviews) {
            Movie filme = review.getMovie();
            if (review.getText() != null) comTexto++;
            if (review.isLiked()) curtidos++;
            if (review.getRating() != null) {
                somaNotas += review.getRating();
                comNota++;
                distribuicao.merge(String.valueOf(review.getRating()), 1, Integer::sum);
            }

            if (filme.getDirector() != null) {
                porDiretor.merge(filme.getDirector().getName(), 1, Integer::sum);
            }
            for (Genre genero : filme.getGenres()) {
                porGenero.merge(genero.getName(), 1, Integer::sum);
            }
            // um ator conta uma vez por filme, mesmo se aparecer duplicado no elenco
            Set<String> atoresDoFilme = new HashSet<>();
            for (MovieCast membro : filme.getCast()) {
                atoresDoFilme.add(membro.getActor().getName());
            }
            for (String ator : atoresDoFilme) {
                porAtor.merge(ator, 1, Integer::sum);
            }

            filmes.add(new Film(new MovieDTO(filme), review.getRating(), review.isLiked(), review.getWatchedOn()));
        }

        Double media = comNota == 0 ? null : Math.round(somaNotas / comNota * 10) / 10.0;

        return new RetrospectiveDTO(
                user.getUsername(),
                user.getName(),
                periodo.toString(),
                reviews.size(),
                comTexto,
                curtidos,
                media,
                distribuicao,
                ranking(porDiretor, TOP_DIRETORES, 1),
                ranking(porGenero, TOP_GENEROS, 1),
                ranking(porAtor, TOP_ATORES, MIN_FILMES_ATOR_RECORRENTE),
                favoritos(filmes),
                filmes);
    }

    /** meses em que a pessoa registrou algum filme, do mais recente para o mais antigo */
    @Transactional(readOnly = true)
    public List<String> mesesDisponiveis(String username) {
        User user = buscarUsuario(username);
        SortedSet<YearMonth> meses = new TreeSet<>(Comparator.reverseOrder());
        for (LocalDate data : reviewRepository.findDatasAssistidas(user)) {
            meses.add(YearMonth.from(data));
        }
        List<String> resultado = new ArrayList<>();
        for (YearMonth mes : meses) resultado.add(mes.toString());
        return resultado;
    }

    // maiores contagens primeiro; empate em ordem alfabética
    private List<RankedItem> ranking(Map<String, Integer> contagem, int limite, int minimo) {
        List<RankedItem> itens = new ArrayList<>();
        for (Map.Entry<String, Integer> e : contagem.entrySet()) {
            if (e.getValue() >= minimo) itens.add(new RankedItem(e.getKey(), e.getValue()));
        }
        itens.sort(Comparator.comparingInt(RankedItem::count).reversed().thenComparing(RankedItem::name));
        return itens.size() > limite ? itens.subList(0, limite) : itens;
    }

    // favoritos: maior nota primeiro; curtida desempata; só entra quem teve nota ou curtida
    private List<Film> favoritos(List<Film> filmes) {
        List<Film> candidatos = new ArrayList<>();
        for (Film f : filmes) {
            if (f.rating() != null || f.liked()) candidatos.add(f);
        }
        candidatos.sort(Comparator
                .comparingDouble((Film f) -> f.rating() == null ? 0 : f.rating()).reversed()
                .thenComparing(Film::liked, Comparator.reverseOrder())
                .thenComparing(Film::watchedOn, Comparator.reverseOrder()));
        return candidatos.size() > TOP_FAVORITOS ? candidatos.subList(0, TOP_FAVORITOS) : candidatos;
    }

    private YearMonth parseMes(String mes) {
        try {
            return YearMonth.parse(mes);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new IllegalArgumentException("Mês inválido, use o formato AAAA-MM (ex.: 2026-10)");
        }
    }

    private User buscarUsuario(String username) {
        return userRepository.findByUsername(username.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado: " + username));
    }
}
