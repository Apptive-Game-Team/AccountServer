package com.wordonline.account.repository;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;

import com.wordonline.account.entity.RefreshTokenEntity;

import reactor.core.publisher.Mono;

public interface RefreshTokenRepository extends R2dbcRepository<RefreshTokenEntity, Long> {

    Mono<RefreshTokenEntity> findByTokenHash(String tokenHash);

    /**
     * Reuse of a spent token means the family leaked, so every token descended from that login
     * dies at once rather than one row per round trip.
     */
    @Modifying
    @Query("UPDATE refresh_token SET revoked_at = :revokedAt "
            + "WHERE family_id = :familyId AND revoked_at IS NULL")
    Mono<Long> revokeFamily(UUID familyId, Instant revokedAt);
}
