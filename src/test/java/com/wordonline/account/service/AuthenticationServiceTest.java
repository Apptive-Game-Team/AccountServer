package com.wordonline.account.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.wordonline.account.config.JwtProvider;
import com.wordonline.account.domain.Member;
import com.wordonline.account.domain.RotatedRefreshToken;
import com.wordonline.account.dto.JoinRequest;
import com.wordonline.account.dto.LoginRequest;
import com.wordonline.account.dto.TokenDelivery;
import com.wordonline.account.util.NicknameGenerator;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MemberService memberService;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private NicknameGenerator nicknameGenerator;

    @Mock
    private RefreshTokenService refreshTokenService;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(passwordEncoder, memberService,
                jwtProvider, nicknameGenerator, refreshTokenService);
    }

    @Test
    void loginWithUnknownEmailFailsWithUnauthorized() {
        when(memberService.getMember("nobody@example.com")).thenReturn(Mono.empty());

        StepVerifier.create(
                        authenticationService.login(
                                new LoginRequest("nobody@example.com", "12345678"),
                                TokenDelivery.BODY))
                .expectErrorSatisfies(error -> {
                    ResponseStatusException statusException = assertInstanceOf(
                            ResponseStatusException.class, error);
                    assertEquals(HttpStatus.UNAUTHORIZED, statusException.getStatusCode());
                })
                .verify();
    }

    @Test
    void refreshRotatesTheTokenAndMintsAFreshAccessToken() {
        Member member = new Member(9L, 1L, "tester", "tester@example.com", "hash", false, List.of());
        when(refreshTokenService.rotate("old-token", TokenDelivery.COOKIE.platform()))
                .thenReturn(Mono.just(new RotatedRefreshToken(9L, "new-token")));
        when(memberService.getMember(9L)).thenReturn(Mono.just(member));
        when(jwtProvider.getJwt(member)).thenReturn("access.jwt");
        when(jwtProvider.getAccessTokenExpirySeconds()).thenReturn(3600L);

        StepVerifier.create(authenticationService.refresh("old-token", TokenDelivery.COOKIE))
                .assertNext(tokens -> {
                    assertEquals("access.jwt", tokens.accessToken());
                    assertEquals("new-token", tokens.refreshToken());
                    assertEquals(3600L, tokens.expiresInSeconds());
                })
                .verifyComplete();
    }

    @Test
    void refreshFailsWithUnauthorizedWhenTheMemberIsGone() {
        when(refreshTokenService.rotate("old-token", TokenDelivery.BODY.platform()))
                .thenReturn(Mono.just(new RotatedRefreshToken(9L, "new-token")));
        when(memberService.getMember(9L)).thenReturn(Mono.empty());

        StepVerifier.create(authenticationService.refresh("old-token", TokenDelivery.BODY))
                .expectErrorSatisfies(error -> {
                    ResponseStatusException statusException = assertInstanceOf(
                            ResponseStatusException.class, error);
                    assertEquals(HttpStatus.UNAUTHORIZED, statusException.getStatusCode());
                })
                .verify();
    }

    @Test
    void theAdminLoginFormDoesNotOpenARefreshTokenFamily() {
        when(memberService.getMember("nobody@example.com")).thenReturn(Mono.empty());

        StepVerifier.create(authenticationService.issueAccessToken(
                        new LoginRequest("nobody@example.com", "12345678")))
                .expectError(ResponseStatusException.class)
                .verify();

        verify(refreshTokenService, never()).issue(anyLong(), anyString());
    }

    @Test
    void guestPasswordIsRandomAndNotDerivedFromEmail() {
        JoinRequest first = authenticationService.getRandomJoinRequest("guest").block();
        JoinRequest second = authenticationService.getRandomJoinRequest("guest").block();

        assertNotNull(first);
        assertNotNull(second);
        assertTrue(first.password().length() >= 8, "guest password must satisfy the 8 char minimum");
        assertNotEquals(first.password(), second.password());
        assertFalse(first.email().contains(first.password()),
                "guest email must not leak the password");
        // email carries no clock reading the password could be rebuilt from
        assertTrue(first.email().matches(
                "guest_[0-9a-f-]{36}@example\\.com"), first.email());
    }

    /**
     * joinGuest() must write the member row with is_guest set rather than leave the flag to be
     * guessed from the generated email later. login() then reads the row back, so the access
     * token it returns carries the guest claim.
     */
    @Test
    void joiningAsAGuestCreatesTheMemberAsAGuest() {
        Member guest = new Member(9L, 1L, "guest", "guest_x@example.com", "hash", true, List.of());
        // The first lookup is the redundant-email check, the second is the login that follows.
        when(memberService.getMember(anyString()))
                .thenReturn(Mono.empty())
                .thenReturn(Mono.just(guest));
        when(memberService.createGuest(any(JoinRequest.class))).thenReturn(Mono.just(guest));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtProvider.getJwt(guest)).thenReturn("access.jwt");
        when(jwtProvider.getAccessTokenExpirySeconds()).thenReturn(3600L);
        when(refreshTokenService.issue(9L, TokenDelivery.BODY.platform()))
                .thenReturn(Mono.just("refresh-token"));

        StepVerifier.create(authenticationService.joinGuest("guest", TokenDelivery.BODY))
                .assertNext(tokens -> {
                    assertEquals("access.jwt", tokens.tokens().accessToken());
                    assertNotNull(tokens.guestPassword());
                })
                .verifyComplete();

        verify(memberService, never()).createMember(any(JoinRequest.class));
    }

    @Test
    void joiningAsARealMemberDoesNotCreateAGuest() {
        Member member = new Member(9L, 1L, "tester", "tester@example.com", "hash", false,
                List.of());
        JoinRequest joinRequest = new JoinRequest("tester@example.com", "tester", "12345678");
        when(memberService.getMember("tester@example.com"))
                .thenReturn(Mono.empty())
                .thenReturn(Mono.just(member));
        when(memberService.createMember(joinRequest)).thenReturn(Mono.just(member));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);
        when(jwtProvider.getJwt(member)).thenReturn("access.jwt");
        when(jwtProvider.getAccessTokenExpirySeconds()).thenReturn(3600L);
        when(refreshTokenService.issue(9L, TokenDelivery.BODY.platform()))
                .thenReturn(Mono.just("refresh-token"));

        StepVerifier.create(authenticationService.join(joinRequest, TokenDelivery.BODY))
                .assertNext(tokens -> assertEquals("access.jwt", tokens.accessToken()))
                .verifyComplete();

        verify(memberService, never()).createGuest(any(JoinRequest.class));
    }
}
