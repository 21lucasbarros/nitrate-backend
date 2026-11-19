package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;

/**
 * A foto de perfil (JPEG quadrado, já redimensionado no upload). Fica numa tabela à parte
 * para os bytes nunca virem junto quando o User é carregado nas listas.
 */
@Entity
@Table(name = "tb_user_avatar")
public class UserAvatar implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    private Long userId;

    @Column(nullable = false)
    private byte[] data;

    public UserAvatar() {
    }

    public UserAvatar(Long userId, byte[] data) {
        this.userId = userId;
        this.data = data;
    }

    public Long getUserId() { return userId; }

    public byte[] getData() { return data; }
    public void setData(byte[] data) { this.data = data; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UserAvatar)) return false;
        return Objects.equals(userId, ((UserAvatar) o).userId);
    }

    @Override
    public int hashCode() { return Objects.hash(userId); }
}
