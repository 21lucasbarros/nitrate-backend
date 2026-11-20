package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** selo concedido pelo admin a um perfil; cada selo no máximo uma vez por pessoa */
@Entity
@Table(name = "tb_user_badge",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "type"}))
public class UserBadge implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    // texto, e não @Enumerated: o Hibernate criaria um check com os valores do enum e não o
    // atualizaria, recusando selos novos (o mesmo problema do tb_notification, ver SchemaAdjustments)
    @Column(nullable = false, length = 40)
    private String type;

    // nota pessoal do admin, mostrada junto do selo (ex.: "Primeiro investidor do Nitrate")
    @Column(length = 140)
    private String note;

    private Instant awardedAt = Instant.now();

    public UserBadge() {
    }

    public UserBadge(User user, BadgeType type, String note) {
        this.user = user;
        this.type = type.name();
        this.note = note;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }

    /** nulo se o selo saiu do catálogo (a linha fica no banco, mas não aparece) */
    public BadgeType getType() {
        try {
            return BadgeType.valueOf(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public Instant getAwardedAt() { return awardedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserBadge)) return false;
        return Objects.equals(id, ((UserBadge) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
