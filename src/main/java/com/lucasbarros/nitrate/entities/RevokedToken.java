package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Token de login encerrado pelo logout antes de vencer, pelo id (jti) do JWT. Fica guardado só até
 * a data em que o token venceria de qualquer forma.
 */
@Entity
@Table(name = "tb_revoked_token")
public class RevokedToken {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false)
    private Instant expiresAt;

    public RevokedToken() {
    }

    public RevokedToken(String id, Instant expiresAt) {
        this.id = id;
        this.expiresAt = expiresAt;
    }

    public String getId() { return id; }
    public Instant getExpiresAt() { return expiresAt; }
}
