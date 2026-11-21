package com.lucasbarros.nitrate.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@BatchSize(size = 50) // em listas, as referências a esta entidade são carregadas em lotes (evita N+1)
@Table(name = "tb_user")
public class User implements Serializable {
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    private Instant createdAt = Instant.now();

    @Column(length = 300)
    private String bio;

    // só o @ (sem URL): o link é montado no front, então ninguém consegue apontar para outro site
    @Column(length = 15)
    private String twitter;

    @Column(length = 30)
    private String instagram;

    // quando a foto de perfil mudou pela última vez; nulo = sem foto (os bytes ficam em UserAvatar)
    private Instant avatarUpdatedAt;

    // última troca de senha; tokens JWT emitidos antes disso deixam de valer (ver JwtService)
    private Instant passwordChangedAt;

    // quando aceitou os Termos e a Política de Privacidade (com a idade mínima); nulo = conta anterior ao aceite
    private Instant termsAcceptedAt;

    // última requisição logada, com precisão de alguns minutos (ver JwtAuthenticationFilter); usado
    // só no painel de admin para contar quem está usando
    private Instant lastSeenAt;

    // fundo do topo do perfil: uma cena de filme escolhida no TMDB (caminho da imagem, filme, título
    // e ano, guardados para não buscar no TMDB a cada visita); nulo = a cena do primeiro favorito
    @Column(length = 100)
    private String coverPath;
    private Long coverTmdbId;
    @Column(length = 300)
    private String coverTitle;
    private Integer coverYear;

    public User() {
    }

    public User(String name, String username, String email, String passwordHash) {
        this.name = name;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getTwitter() { return twitter; }
    public void setTwitter(String twitter) { this.twitter = twitter; }

    public String getInstagram() { return instagram; }
    public void setInstagram(String instagram) { this.instagram = instagram; }

    public Instant getAvatarUpdatedAt() { return avatarUpdatedAt; }
    public void setAvatarUpdatedAt(Instant avatarUpdatedAt) { this.avatarUpdatedAt = avatarUpdatedAt; }

    public Instant getPasswordChangedAt() { return passwordChangedAt; }
    public void setPasswordChangedAt(Instant passwordChangedAt) { this.passwordChangedAt = passwordChangedAt; }

    public Instant getTermsAcceptedAt() { return termsAcceptedAt; }
    public void setTermsAcceptedAt(Instant termsAcceptedAt) { this.termsAcceptedAt = termsAcceptedAt; }

    public Instant getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public String getCoverPath() { return coverPath; }
    public void setCoverPath(String coverPath) { this.coverPath = coverPath; }

    public Long getCoverTmdbId() { return coverTmdbId; }
    public void setCoverTmdbId(Long coverTmdbId) { this.coverTmdbId = coverTmdbId; }

    public String getCoverTitle() { return coverTitle; }
    public void setCoverTitle(String coverTitle) { this.coverTitle = coverTitle; }

    public Integer getCoverYear() { return coverYear; }
    public void setCoverYear(Integer coverYear) { this.coverYear = coverYear; }

    /** caminho da foto na API, com a versão na URL para o cache; nulo sem foto */
    public String getAvatarPath() {
        return avatarUpdatedAt == null ? null : "/users/" + username + "/avatar?v=" + avatarUpdatedAt.toEpochMilli();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof User)) return false;
        return Objects.equals(id, ((User) o).id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}