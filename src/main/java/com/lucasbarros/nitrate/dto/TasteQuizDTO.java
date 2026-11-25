package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.taste.TasteQuiz;

import java.util.List;

/** GET /taste/quiz: o questionário do onboarding, sem os filtros internos de cada resposta */
public record TasteQuizDTO(List<Item> genres, List<Question> questions) {

    public record Item(String id, String label) {
    }

    public record Option(String id, String label, String hint) {
    }

    /** `genre`: o id do gênero que abre esta pergunta */
    public record Question(String id, String genre, String title, List<Option> options) {
    }

    public static TasteQuizDTO doQuestionario() {
        return new TasteQuizDTO(
                TasteQuiz.GENEROS.stream().map(g -> new Item(g.id(), g.label())).toList(),
                TasteQuiz.PERGUNTAS.stream()
                        .map(p -> new Question(p.id(), p.genre(), p.title(),
                                p.options().stream().map(o -> new Option(o.id(), o.label(), o.hint())).toList()))
                        .toList());
    }
}
