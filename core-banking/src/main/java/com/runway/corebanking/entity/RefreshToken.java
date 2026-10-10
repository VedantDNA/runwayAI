package com.runway.corebanking.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * A long-lived refresh token used to obtain new short-lived access tokens.
 *
 * <p><b>Security design</b>: Only the SHA-256 hash of the token is stored here.
 * The actual token value is sent to the client exactly once (on login/register)
 * and never stored in plaintext. If this table is breached, the attacker gets
 * hashes — useless without the originals.
 *
 * <p>Token rotation: on every /auth/refresh call, the old token is revoked
 * (revoked_at set) and a new one is issued. This bounds exposure time to one
 * refresh cycle if a token is silently stolen.
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** SHA-256 of the raw token. Compare with: SHA-256(incoming token) == this. */
    @Column(name = "token_hash", nullable = false, unique = true, length = 255)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    /** Null = still valid. Set when the user logs out or token is rotated. */
    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(expiresAt);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isValid() {
        return !isExpired() && !isRevoked();
    }
}
