package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.PasswordResetToken;
import com.lucasbarros.nitrate.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("delete from PasswordResetToken t where t.user = :user")
    void deleteAllByUser(@Param("user") User user);

    @Modifying
    @Query("delete from PasswordResetToken t where t.expiresAt < :agora")
    void deleteAllExpiredBefore(@Param("agora") Instant agora);
}
