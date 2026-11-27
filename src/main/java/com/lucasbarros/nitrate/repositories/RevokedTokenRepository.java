package com.lucasbarros.nitrate.repositories;

import com.lucasbarros.nitrate.entities.RevokedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface RevokedTokenRepository extends JpaRepository<RevokedToken, String> {

    @Modifying
    @Query("delete from RevokedToken t where t.expiresAt < :agora")
    void deleteAllExpiredBefore(@Param("agora") Instant agora);
}
