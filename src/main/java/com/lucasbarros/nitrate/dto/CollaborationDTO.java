package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.ReviewCollaboration;

// o convite de review em conjunto, para quem foi convidado decidir: a review de quem convidou e,
// se já aceitou, a review que ficou para ele
public record CollaborationDTO(Long id, ReviewCollaboration.Status status, UserSummaryDTO author,
                               UserSummaryDTO invitee, ReviewDTO review, Long inviteeReviewId) {

    public CollaborationDTO(ReviewCollaboration c) {
        this(c.getId(), c.getStatus(), new UserSummaryDTO(c.getAuthor()), new UserSummaryDTO(c.getInvitee()),
                new ReviewDTO(c.getAuthorReview()),
                c.getInviteeReview() == null ? null : c.getInviteeReview().getId());
    }
}
