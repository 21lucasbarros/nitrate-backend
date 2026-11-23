package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.SeriesReviewCollaboration;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeriesReviewCollaborationRepository extends JpaRepository<SeriesReviewCollaboration, Long> {

    // exclusão de conta: os registros das duas pessoas deixam de apontar para as colaborações dela...
    @Modifying
    @Query("update SeriesReview r set r.collaboration = null where r.collaboration in " +
            "(select c from SeriesReviewCollaboration c where c.author = :user or c.invitee = :user)")
    void desligarReviewsDe(@Param("user") User user);

    // ...e as colaborações somem (as notificações delas já foram junto com as da pessoa)
    @Modifying
    @Query("delete from SeriesReviewCollaboration c where c.author = :user or c.invitee = :user")
    void deleteAllInvolving(@Param("user") User user);
}
