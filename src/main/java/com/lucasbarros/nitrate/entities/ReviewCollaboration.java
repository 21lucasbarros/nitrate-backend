package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/**
 * Review em conjunto: quem registrou o filme marca com quem assistiu (quantas pessoas quiser, um
 * convite para cada), e cada pessoa marcada aceita (ganhando a própria review do filme, ligada à
 * de quem convidou) ou recusa. Ao aceitar, ela escolhe entre escrever a dela ou usar a de quem
 * convidou (`sharedText`). A sessão é a review de quem convidou com os convites aceitos dela.
 */
@Entity
@Table(name = "tb_review_collaboration",
        uniqueConstraints = @UniqueConstraint(columnNames = {"author_review_id", "invitee_id"}))
public class ReviewCollaboration implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Status { PENDING, ACCEPTED, DECLINED }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "author_id")
    private User author;

    // a review de quem convidou: um convite por pessoa marcada nela
    @ManyToOne(optional = false)
    @JoinColumn(name = "author_review_id")
    private Review authorReview;

    @ManyToOne(optional = false)
    @JoinColumn(name = "invitee_id")
    private User invitee;

    // criada quando a pessoa aceita; nula enquanto o convite está pendente ou se ela recusou
    @OneToOne
    @JoinColumn(name = "invitee_review_id", unique = true)
    private Review inviteeReview;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Status status = Status.PENDING;

    // quem aceitou usou o texto de quem convidou: as edições do texto original chegam à cópia,
    // até a pessoa escrever o próprio texto
    @Column(nullable = false, columnDefinition = "boolean not null default false")
    private boolean sharedText;

    private Instant createdAt = Instant.now();

    private Instant respondedAt;

    public ReviewCollaboration() {
    }

    public ReviewCollaboration(Review authorReview, User invitee) {
        this.author = authorReview.getUser();
        this.authorReview = authorReview;
        this.invitee = invitee;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getAuthor() { return author; }
    public Review getAuthorReview() { return authorReview; }
    public User getInvitee() { return invitee; }

    public Review getInviteeReview() { return inviteeReview; }
    public void setInviteeReview(Review inviteeReview) { this.inviteeReview = inviteeReview; }

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
        if (!(o instanceof ReviewCollaboration)) return false;
        return Objects.equals(id, ((ReviewCollaboration) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
