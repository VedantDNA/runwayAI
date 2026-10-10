package com.runway.corebanking.repository;

import com.runway.corebanking.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Revoke all tokens for a user — called on logout or password change. */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revokedAt = CURRENT_TIMESTAMP WHERE rt.user.id = :userId AND rt.revokedAt IS NULL")
    int revokeAllForUser(@Param("userId") UUID userId);

    /** Clean up expired/revoked tokens (called by a scheduled job). */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < CURRENT_TIMESTAMP OR rt.revokedAt IS NOT NULL")
    int deleteExpiredAndRevoked();
}
