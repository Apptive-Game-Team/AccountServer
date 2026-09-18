package com.wordonline.account.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.wordonline.account.entity.RefreshTokenEntity;
import com.wordonline.account.repository.RefreshTokenRepository;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    private static final String PRESENTED_TOKEN = "presented-refresh-token";
    private static final long MEMBER_ID = 7L;
    private static final String PLATFORM = "body";

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    private RefreshTokenService refreshTokenService;
    private UUID familyId;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository);
        familyId = UUID.randomUUID();
    }

    @Test
    void issuedTokenIsOpaqueAndOnlyItsHashIsStored() {
        savesWhateverItIsGiven();

        String token = refreshTokenService.issue(MEMBER_ID, PLATFORM).block();

        assertThat(token).isNotNull().doesNotContain(".");
        RefreshTokenEntity stored = savedTokens().getFirst();
        assertThat(stored.getTokenHash())
                .isEqualTo(RefreshTokenService.hash(token))
                .hasSize(64)
                .isNotEqualTo(token);
        assertThat(stored.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(stored.getFamilyId()).isNotNull();
        assertThat(stored.getExpiresAt())
                .isAfter(stored.getIssuedAt().plus(RefreshTokenService.LIFETIME).minusSeconds(5));
    }

    @Test
    void rotationSpendsThePresentedTokenAndIssuesASuccessorInTheSameFamily() {
        Instant issuedAt = Instant.now().minusSeconds(3600);
        presents(token(issuedAt, null, null));
        savesWhateverItIsGiven();

        var rotated = refreshTokenService.rotate(PRESENTED_TOKEN, PLATFORM).block();

        assertThat(rotated).isNotNull();
        assertThat(rotated.memberId()).isEqualTo(MEMBER_ID);
        assertThat(rotated.refreshToken()).isNotEqualTo(PRESENTED_TOKEN);

        List<RefreshTokenEntity> saved = savedTokens();
        assertThat(saved).hasSize(2);
        RefreshTokenEntity spent = saved.getFirst();
        assertThat(spent.getRotatedAt()).isNotNull();
        assertThat(spent.getTokenHash()).isEqualTo(RefreshTokenService.hash(PRESENTED_TOKEN));

        RefreshTokenEntity successor = saved.get(1);
        assertThat(successor.getId()).isNull();
        assertThat(successor.getFamilyId()).isEqualTo(familyId);
        assertThat(successor.getTokenHash())
                .isEqualTo(RefreshTokenService.hash(rotated.refreshToken()));
        verify(refreshTokenRepository, never()).revokeFamily(any(), any());
    }

    @Test
    void aRetryInsideTheGraceWindowSucceedsWithoutRevokingTheFamily() {
        Instant rotatedAt = Instant.now().minus(RefreshTokenService.ROTATION_GRACE).plusSeconds(4);
        presents(token(Instant.now().minusSeconds(3600), rotatedAt, null));
        savesWhateverItIsGiven();

        var rotated = refreshTokenService.rotate(PRESENTED_TOKEN, PLATFORM).block();

        assertThat(rotated).isNotNull();
        assertThat(rotated.refreshToken()).isNotBlank();
        verify(refreshTokenRepository, never()).revokeFamily(any(), any());
        // The row was already spent, so its rotated_at stays where the first rotation put it.
        assertThat(savedTokens().getFirst().getRotatedAt()).isEqualTo(rotatedAt);
    }

    @Test
    void reuseBeyondTheGraceWindowRevokesTheWholeFamilyAndFails() {
        Instant rotatedAt = Instant.now().minus(RefreshTokenService.ROTATION_GRACE)
                .minusSeconds(1);
        presents(token(Instant.now().minusSeconds(3600), rotatedAt, null));
        when(refreshTokenRepository.revokeFamily(eq(familyId), any())).thenReturn(Mono.just(2L));

        expectsUnauthorized(refreshTokenService.rotate(PRESENTED_TOKEN, PLATFORM));

        verify(refreshTokenRepository).revokeFamily(eq(familyId), any());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void anUnknownTokenIsRejected() {
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(PRESENTED_TOKEN)))
                .thenReturn(Mono.empty());

        expectsUnauthorized(refreshTokenService.rotate(PRESENTED_TOKEN, PLATFORM));

        verify(refreshTokenRepository, never()).save(any());
        verify(refreshTokenRepository, never()).revokeFamily(any(), any());
    }

    @Test
    void aRevokedTokenIsRejectedWithoutTouchingTheFamilyAgain() {
        presents(token(Instant.now().minusSeconds(3600), null, Instant.now().minusSeconds(60)));

        expectsUnauthorized(refreshTokenService.rotate(PRESENTED_TOKEN, PLATFORM));

        verify(refreshTokenRepository, never()).save(any());
        verify(refreshTokenRepository, never()).revokeFamily(any(), any());
    }

    @Test
    void anExpiredTokenIsRejected() {
        RefreshTokenEntity expired = new RefreshTokenEntity(11L, familyId, MEMBER_ID,
                RefreshTokenService.hash(PRESENTED_TOKEN), PLATFORM,
                Instant.now().minus(RefreshTokenService.LIFETIME).minusSeconds(60),
                Instant.now().minusSeconds(60), null, null, null);
        presents(expired);

        expectsUnauthorized(refreshTokenService.rotate(PRESENTED_TOKEN, PLATFORM));

        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void aMissingTokenIsRejectedBeforeTheDatabaseIsAsked() {
        expectsUnauthorized(refreshTokenService.rotate(null, PLATFORM));
        expectsUnauthorized(refreshTokenService.rotate("  ", PLATFORM));

        verify(refreshTokenRepository, never()).findByTokenHash(any());
    }

    @Test
    void logoutRevokesTheFamilyOfThePresentedToken() {
        presents(token(Instant.now().minusSeconds(3600), null, null));
        when(refreshTokenRepository.revokeFamily(eq(familyId), any())).thenReturn(Mono.just(1L));

        StepVerifier.create(refreshTokenService.revoke(PRESENTED_TOKEN)).verifyComplete();

        verify(refreshTokenRepository).revokeFamily(eq(familyId), any());
    }

    @Test
    void logoutWithAnUnknownTokenStillCompletes() {
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(PRESENTED_TOKEN)))
                .thenReturn(Mono.empty());

        StepVerifier.create(refreshTokenService.revoke(PRESENTED_TOKEN)).verifyComplete();

        verify(refreshTokenRepository, never()).revokeFamily(any(), any());
    }

    private RefreshTokenEntity token(Instant issuedAt, Instant rotatedAt, Instant revokedAt) {
        return new RefreshTokenEntity(11L, familyId, MEMBER_ID,
                RefreshTokenService.hash(PRESENTED_TOKEN), PLATFORM, issuedAt,
                issuedAt.plus(RefreshTokenService.LIFETIME), rotatedAt, revokedAt, null);
    }

    private void presents(RefreshTokenEntity stored) {
        when(refreshTokenRepository.findByTokenHash(RefreshTokenService.hash(PRESENTED_TOKEN)))
                .thenReturn(Mono.just(stored));
    }

    private void savesWhateverItIsGiven() {
        when(refreshTokenRepository.save(any(RefreshTokenEntity.class)))
                .thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));
    }

    private List<RefreshTokenEntity> savedTokens() {
        ArgumentCaptor<RefreshTokenEntity> captor = ArgumentCaptor.forClass(
                RefreshTokenEntity.class);
        verify(refreshTokenRepository, org.mockito.Mockito.atLeastOnce()).save(captor.capture());
        return captor.getAllValues();
    }

    private static void expectsUnauthorized(Mono<?> call) {
        StepVerifier.create(call)
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOfSatisfying(ResponseStatusException.class, statusException ->
                                assertThat(statusException.getStatusCode())
                                        .isEqualTo(HttpStatus.UNAUTHORIZED)))
                .verify();
    }
}
