package com.wordonline.account.entity;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * One issued refresh token. {@code tokenHash} is the SHA-256 hex of the token; the token itself
 * goes to the client once and is never stored.
 */
@Table("refresh_token")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class RefreshTokenEntity {

    @Id
    private Long id;
    private UUID familyId;
    private Long memberId;
    private String tokenHash;
    private String platform;
    private Instant issuedAt;
    private Instant expiresAt;
    private Instant rotatedAt;
    private Instant revokedAt;
    private Instant lastUsedAt;

    public static RefreshTokenEntity issued(
            UUID familyId,
            long memberId,
            String tokenHash,
            String platform,
            Instant issuedAt,
            Instant expiresAt
    ) {
        return new RefreshTokenEntity(null, familyId, memberId, tokenHash, platform, issuedAt,
                expiresAt, null, null, null);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isExpiredAt(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public boolean isRotated() {
        return rotatedAt != null;
    }

    /**
     * A spent token presented again this soon after rotation is a client retrying over a flaky
     * network, not an attacker replaying a stolen token.
     */
    public boolean isWithinRotationGrace(Instant now, Duration grace) {
        return rotatedAt != null && Duration.between(rotatedAt, now).compareTo(grace) <= 0;
    }

    public RefreshTokenEntity markRotated(Instant now) {
        return new RefreshTokenEntity(id, familyId, memberId, tokenHash, platform, issuedAt,
                expiresAt, now, revokedAt, now);
    }

    public RefreshTokenEntity markUsed(Instant now) {
        return new RefreshTokenEntity(id, familyId, memberId, tokenHash, platform, issuedAt,
                expiresAt, rotatedAt, revokedAt, now);
    }
}
