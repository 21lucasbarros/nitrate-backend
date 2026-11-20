package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.Objects;

/**
 * Pedido de redefinição de senha. Guarda só o SHA-256 do token: o token em si vai no link do
 * e-mail, então quem ler o banco não consegue usá-lo.
 */
@Entity
@Table(name = "tb_password_reset_token")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant createdAt = Instant.now();

    public PasswordResetToken() {
    }

    public PasswordResetToken(User user, String tokenHash, Instant expiresAt) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public String getTokenHash() { return tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean expirado(Instant agora) {
        return !expiresAt.isAfter(agora);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PasswordResetToken)) return false;
        return Objects.equals(id, ((PasswordResetToken) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
