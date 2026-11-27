package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.ReviewCollaboration;
import com.lucasbarros.nitrate.security.UsuarioAtual;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReviewDTO {
    private Long id;
    private MovieDTO movie;
    private boolean customPoster; // movie.posterPath é o pôster escolhido pela pessoa, e não o do filme
    private UserSummaryDTO user;
    private Double rating;
    private String text;
    private boolean liked;
    private boolean spoiler;
    private boolean rewatch;
    private LocalDate watchedOn;
    private Instant createdAt;
    private Together together;

    /**
     * Review em conjunto, vista do lado do dono desta review: `partners` são as outras pessoas da
     * sessão, com a review de cada uma (nula enquanto o convite está pendente). Quem convidou vê
     * os convites pendentes (só ele: para os outros, a pessoa só aparece depois de aceitar); quem
     * aceitou vê quem convidou e quem mais aceitou. `invitedBy` é quem
     * convidou (nulo para ele mesmo) e `sharedText` = o texto desta review é o de `invitedBy`.
     * Convites recusados não aparecem.
     */
    public record Together(List<Partner> partners, boolean invitedByMe, UserSummaryDTO invitedBy,
                           boolean sharedText) {

        static Together de(Review review) {
            ReviewCollaboration recebido = review.getCollaboration();
            List<Partner> parceiros = new ArrayList<>();
            if (recebido == null) {
                Long eu = UsuarioAtual.id();
                boolean dono = eu != null && eu.equals(review.getUser().getId());
                for (ReviewCollaboration c : review.getInvites()) {
                    if (c.getStatus() == ReviewCollaboration.Status.ACCEPTED
                            || (dono && c.getStatus() == ReviewCollaboration.Status.PENDING)) parceiros.add(new Partner(c));
                }
                return parceiros.isEmpty() ? null : new Together(parceiros, true, null, false);
            }
            Review origem = recebido.getAuthorReview();
            parceiros.add(new Partner(new UserSummaryDTO(origem.getUser()), origem.getId(),
                    ReviewCollaboration.Status.ACCEPTED));
            for (ReviewCollaboration c : origem.getInvites()) {
                if (!c.equals(recebido) && c.getStatus() == ReviewCollaboration.Status.ACCEPTED) parceiros.add(new Partner(c));
            }
            return new Together(parceiros, false, new UserSummaryDTO(origem.getUser()), recebido.isSharedText());
        }
    }

    // uma pessoa da sessão em conjunto; `reviewId` nulo enquanto o convite dela está pendente
    public record Partner(UserSummaryDTO user, Long reviewId, ReviewCollaboration.Status status) {
        Partner(ReviewCollaboration c) {
            this(new UserSummaryDTO(c.getInvitee()),
                    c.getInviteeReview() == null ? null : c.getInviteeReview().getId(), c.getStatus());
        }
    }

    public ReviewDTO(Review entity) {
        id = entity.getId();
        movie = new MovieDTO(entity.getMovie());
        // o pôster que a pessoa escolheu aparece no lugar do pôster do filme em tudo que mostra a review
        customPoster = entity.getPosterPath() != null;
        if (customPoster) movie.setPosterPath(entity.getPosterPath());
        user = new UserSummaryDTO(entity.getUser());
        rating = entity.getRating();
        text = entity.getText();
        liked = entity.isLiked();
        spoiler = entity.isSpoiler();
        rewatch = entity.isRewatch();
        watchedOn = entity.getWatchedOn();
        createdAt = entity.getCreatedAt();
        together = Together.de(entity);
    }

    public Long getId() { return id; }
    public MovieDTO getMovie() { return movie; }
    public boolean isCustomPoster() { return customPoster; }
    public UserSummaryDTO getUser() { return user; }
    public Double getRating() { return rating; }
    public String getText() { return text; }
    public boolean isLiked() { return liked; }
    public boolean isRewatch() { return rewatch; }
    public boolean isSpoiler() { return spoiler; }
    public LocalDate getWatchedOn() { return watchedOn; }
    public Instant getCreatedAt() { return createdAt; }
    public Together getTogether() { return together; }
}
