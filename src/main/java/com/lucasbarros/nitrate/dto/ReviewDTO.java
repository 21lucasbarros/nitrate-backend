package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.Review;
import com.lucasbarros.nitrate.entities.ReviewCollaboration;

import java.time.Instant;
import java.time.LocalDate;

public class ReviewDTO {
    private Long id;
    private MovieDTO movie;
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
     * Review em conjunto, vista do lado do dono desta review: `partner` é a outra pessoa e
     * `partnerReviewId` a review dela (nula enquanto o convite está pendente). `sharedText` = o
     * texto desta review é o de `partner` (quem aceitou usou a review de quem convidou).
     * Convites recusados não aparecem.
     */
    public record Together(UserSummaryDTO partner, Long partnerReviewId, ReviewCollaboration.Status status,
                           boolean invitedByMe, boolean sharedText) {
    }

    public ReviewDTO(Review entity) {
        id = entity.getId();
        movie = new MovieDTO(entity.getMovie());
        user = new UserSummaryDTO(entity.getUser());
        rating = entity.getRating();
        text = entity.getText();
        liked = entity.isLiked();
        spoiler = entity.isSpoiler();
        rewatch = entity.isRewatch();
        watchedOn = entity.getWatchedOn();
        createdAt = entity.getCreatedAt();
        ReviewCollaboration c = entity.getCollaboration();
        if (c != null && c.getStatus() != ReviewCollaboration.Status.DECLINED) {
            Review parceira = c.reviewParceiraDe(entity);
            boolean convidei = entity.equals(c.getAuthorReview());
            together = new Together(new UserSummaryDTO(c.parceiroDe(entity)),
                    parceira == null ? null : parceira.getId(), c.getStatus(), convidei,
                    !convidei && c.isSharedText());
        }
    }

    public Long getId() { return id; }
    public MovieDTO getMovie() { return movie; }
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
