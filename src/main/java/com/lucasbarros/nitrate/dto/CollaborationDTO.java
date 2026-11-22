package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.ReviewCollaboration;

import java.util.List;

// o convite de review em conjunto, para quem foi convidado decidir: a review de quem convidou,
// as outras pessoas marcadas na mesma sessão (`companions`, sem quem recusou) e, se já aceitou,
// a review que ficou para ele
public record CollaborationDTO(Long id, ReviewCollaboration.Status status, UserSummaryDTO author,
                               UserSummaryDTO invitee, ReviewDTO review, Long inviteeReviewId,
                               List<ReviewDTO.Partner> companions) {

    public CollaborationDTO(ReviewCollaboration c) {
        this(c.getId(), c.getStatus(), new UserSummaryDTO(c.getAuthor()), new UserSummaryDTO(c.getInvitee()),
                new ReviewDTO(c.getAuthorReview()),
                c.getInviteeReview() == null ? null : c.getInviteeReview().getId(),
                c.getAuthorReview().getInvites().stream()
                        .filter(o -> !o.equals(c) && o.getStatus() != ReviewCollaboration.Status.DECLINED)
                        .map(ReviewDTO.Partner::new)
                        .toList());
    }
}
