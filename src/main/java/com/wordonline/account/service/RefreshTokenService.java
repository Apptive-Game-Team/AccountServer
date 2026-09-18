package com.wordonline.account.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.wordonline.account.domain.RotatedRefreshToken;
import com.wordonline.account.entity.RefreshTokenEntity;
import com.wordonline.account.repository.RefreshTokenRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Issues, rotates and revokes refresh tokens.
 *
 * <p>The token is 32 random bytes rather than a JWT, because a signed token cannot be taken
 * back before it expires and this one has to be revocable. Only its SHA-256 hex reaches the
 * database.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RefreshTokenService {

    static final Duration LIFETIME = Duration.ofDays(60);

    /**
     * A client that retries a refresh after a dropped response presents a token the server has
     * already spent. Without this window that ordinary retry would look like a stolen token and
     * would log a real user out of every session.
     */
    static final Duration ROTATION_GRACE = Duration.ofSeconds(10);

    private static final String REJECT_MESSAGE = "refresh token 이 유효하지 않습니다.";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final int TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final RefreshTokenRepository refreshTokenRepository;

    /** Opens a new family. One login is one family. */
    public Mono<String> issue(long memberId, String platform) {
        return issueInFamily(UUID.randomUUID(), memberId, platform);
    }

    public Mono<RotatedRefreshToken> rotate(String presentedToken, String platform) {
        if (presentedToken == null || presentedToken.isBlank()) {
            return Mono.error(RefreshTokenService::rejected);
        }
        Instant now = Instant.now();
        return refreshTokenRepository.findByTokenHash(hash(presentedToken))
                .switchIfEmpty(Mono.error(RefreshTokenService::rejected))
                .flatMap(presented -> rotateChecked(presented, platform, now));
    }

    /**
     * Logout. The whole family goes, not just the row presented: a token issued inside the
     * retry grace window is a live sibling of the presented one, and leaving it alive would
     * mean logout did not log the caller out.
     */
    public Mono<Void> revoke(String presentedToken) {
        if (presentedToken == null || presentedToken.isBlank()) {
            return Mono.empty();
        }
        Instant now = Instant.now();
        return refreshTokenRepository.findByTokenHash(hash(presentedToken))
                .flatMap(presented -> refreshTokenRepository.revokeFamily(
                        presented.getFamilyId(), now))
                .then();
    }

    private Mono<RotatedRefreshToken> rotateChecked(
            RefreshTokenEntity presented,
            String platform,
            Instant now
    ) {
        if (presented.isRevoked() || presented.isExpiredAt(now)) {
            return Mono.error(rejected());
        }
        if (presented.isRotated()) {
            return rotateSpent(presented, platform, now);
        }
        return refreshTokenRepository.save(presented.markRotated(now))
                .then(successorOf(presented, platform));
    }

    private Mono<RotatedRefreshToken> rotateSpent(
            RefreshTokenEntity presented,
            String platform,
            Instant now
    ) {
        if (!presented.isWithinRotationGrace(now, ROTATION_GRACE)) {
            log.warn("[REFRESH TOKEN REUSE] member {} family {} revoked",
                    presented.getMemberId(), presented.getFamilyId());
            return refreshTokenRepository.revokeFamily(presented.getFamilyId(), now)
                    .then(Mono.error(rejected()));
        }
        // Already spent, so rotated_at stays where it is and only the use is recorded.
        return refreshTokenRepository.save(presented.markUsed(now))
                .then(successorOf(presented, platform));
    }

    private Mono<RotatedRefreshToken> successorOf(RefreshTokenEntity presented, String platform) {
        return issueInFamily(presented.getFamilyId(), presented.getMemberId(), platform)
                .map(successor -> new RotatedRefreshToken(presented.getMemberId(), successor));
    }

    private Mono<String> issueInFamily(UUID familyId, long memberId, String platform) {
        return Mono.defer(() -> {
            String token = generateToken();
            Instant now = Instant.now();
            return refreshTokenRepository.save(RefreshTokenEntity.issued(
                            familyId, memberId, hash(token), platform, now, now.plus(LIFETIME)))
                    .thenReturn(token);
        });
    }

    private static String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
            return HexFormat.of()
                    .formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(HASH_ALGORITHM + " is missing from this JVM", e);
        }
    }

    /** Unknown, expired, revoked and reused all answer the same, so nothing is an oracle. */
    private static ResponseStatusException rejected() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, REJECT_MESSAGE);
    }
}
