package com.lucasbarros.nitrate.dto;

import com.lucasbarros.nitrate.entities.ReviewCollaboration;
import com.lucasbarros.nitrate.entities.SeriesReviewCollaboration;

import java.util.List;

// o convite de registro de série em conjunto, no formato do CollaborationDTO dos filmes: o registro de
// quem convidou, as outras pessoas marcadas na mesma sessão (sem quem recusou) e, se já aceitou, o
// registro que ficou para ele
public record SeriesCollaborationDTO(Long id, ReviewCollaboration.Status status, UserSummaryDTO author,
                                     UserSummaryDTO invitee, SeriesReviewDTO review, Long inviteeReviewId,
                                     List<ReviewDTO.Partner> companions) {

    public SeriesCollaborationDTO(SeriesReviewCollaboration c) {
        this(c.getId(), c.getStatus(), new UserSummaryDTO(c.getAuthor()), new UserSummaryDTO(c.getInvitee()),
                new SeriesReviewDTO(c.getAuthorReview()),
                c.getInviteeReview() == null ? null : c.getInviteeReview().getId(),
                c.getAuthorReview().getInvites().stream()
                        .filter(o -> !o.equals(c) && o.getStatus() != ReviewCollaboration.Status.DECLINED)
                        .map(SeriesReviewDTO::partner)
                        .toList());
    }
}
