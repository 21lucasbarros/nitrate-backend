package com.lucasbarros.nitrate.entities;

import com.lucasbarros.nitrate.entities.ReviewCollaboration.Status;
import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Registro de série em conjunto, como o ReviewCollaboration dos filmes: quem registrou marca com quem
 * assistiu (um convite por pessoa), e cada pessoa marcada aceita (ganhando o próprio registro da mesma
 * série, temporada ou episódio, ligado ao de quem convidou) ou recusa.
 */
@Entity
@Table(name = "tb_series_review_collaboration",
        uniqueConstraints = @UniqueConstraint(columnNames = {"author_review_id", "invitee_id"}))
public class SeriesReviewCollaboration implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    // o registro de quem convidou: um convite por pessoa marcada nele
    @ManyToOne(optional = false)
    @JoinColumn(name = "author_review_id")
    private SeriesReview authorReview;

    @ManyToOne(optional = false)
    @JoinColumn(name = "invitee_id")
    private User invitee;

    // criado quando a pessoa aceita; nulo enquanto o convite está pendente ou se ela recusou
    @OneToOne
    @JoinColumn(name = "invitee_review_id", unique = true)
    private SeriesReview inviteeReview;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.PENDING;

    // quem aceitou usou o texto de quem convidou: as edições do texto original chegam à cópia,
    // até a pessoa escrever o próprio texto
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean sharedText;

    private Instant createdAt = Instant.now();

    private Instant respondedAt;

    public SeriesReviewCollaboration() {
    }

    public SeriesReviewCollaboration(SeriesReview authorReview, User invitee) {
        this.author = authorReview.getUser();
        this.authorReview = authorReview;
        this.invitee = invitee;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getAuthor() { return author; }
    public SeriesReview getAuthorReview() { return authorReview; }
    public User getInvitee() { return invitee; }

    public SeriesReview getInviteeReview() { return inviteeReview; }
    public void setInviteeReview(SeriesReview inviteeReview) { this.inviteeReview = inviteeReview; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public boolean isSharedText() { return sharedText; }
    public void setSharedText(boolean sharedText) { this.sharedText = sharedText; }

    public Instant getCreatedAt() { return createdAt; }

    public Instant getRespondedAt() { return respondedAt; }
    public void setRespondedAt(Instant respondedAt) { this.respondedAt = respondedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SeriesReviewCollaboration)) return false;
        return Objects.equals(id, ((SeriesReviewCollaboration) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
