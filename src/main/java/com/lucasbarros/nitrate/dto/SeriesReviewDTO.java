package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.ReviewCollaboration.Status;
import com.lucasbarros.nitrate.entities.SeriesReview;
import com.lucasbarros.nitrate.entities.SeriesReviewCollaboration;
import com.lucasbarros.nitrate.security.UsuarioAtual;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public record SeriesReviewDTO(
        Long id,
        SeriesDTO series, // com o pôster escolhido pela pessoa no lugar do da série, quando houver
        boolean customPoster,
        UserSummaryDTO user,
        Integer seasonNumber, // nulo = a série inteira
        Integer episodeNumber, // nulo = a temporada inteira
        Double rating,
        String text,
        boolean liked,
        boolean spoiler,
        boolean rewatch,
        LocalDate watchedOn,
        Instant createdAt,
        ReviewDTO.Together together) { // registro em conjunto, no mesmo formato das reviews de filme

    public SeriesReviewDTO(SeriesReview r) {
        this(r.getId(), new SeriesDTO(r.getSeries()).comPoster(r.getPosterPath()), r.getPosterPath() != null,
                new UserSummaryDTO(r.getUser()), r.getSeasonNumber(),
                r.getEpisodeNumber(), r.getRating(), r.getText(), r.isLiked(), r.isSpoiler(), r.isRewatch(),
                r.getWatchedOn(), r.getCreatedAt(), together(r));
    }

    // como ReviewDTO.Together.de: quem convidou vê os convites pendentes (só ele); quem aceitou vê quem
    // convidou e quem mais aceitou. Convites recusados não aparecem
    private static ReviewDTO.Together together(SeriesReview review) {
        SeriesReviewCollaboration recebido = review.getCollaboration();
        List<ReviewDTO.Partner> parceiros = new ArrayList<>();
        if (recebido == null) {
            Long eu = UsuarioAtual.id();
            boolean dono = eu != null && eu.equals(review.getUser().getId());
            for (SeriesReviewCollaboration c : review.getInvites()) {
                if (c.getStatus() == Status.ACCEPTED || (dono && c.getStatus() == Status.PENDING)) parceiros.add(partner(c));
            }
            return parceiros.isEmpty() ? null : new ReviewDTO.Together(parceiros, true, null, false);
        }
        SeriesReview origem = recebido.getAuthorReview();
        parceiros.add(new ReviewDTO.Partner(new UserSummaryDTO(origem.getUser()), origem.getId(), Status.ACCEPTED));
        for (SeriesReviewCollaboration c : origem.getInvites()) {
            if (!c.equals(recebido) && c.getStatus() == Status.ACCEPTED) parceiros.add(partner(c));
        }
        return new ReviewDTO.Together(parceiros, false, new UserSummaryDTO(origem.getUser()), recebido.isSharedText());
    }

    static ReviewDTO.Partner partner(SeriesReviewCollaboration c) {
        return new ReviewDTO.Partner(new UserSummaryDTO(c.getInvitee()),
                c.getInviteeReview() == null ? null : c.getInviteeReview().getId(), c.getStatus());
    }
}
