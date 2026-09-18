package com.wordonline.account.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.server.ResponseStatusException;

import com.wordonline.account.domain.GuestTokens;
import com.wordonline.account.domain.IssuedTokens;
import com.wordonline.account.dto.TokenDelivery;
import com.wordonline.account.service.AuthenticationService;

import reactor.core.publisher.Mono;

/**
 * The wire contract the game clients are written against.
 */
class AuthenticationControllerTest {

    private static final String LOGIN_BODY = """
            {"email":"tester@example.com","password":"12345678"}""";
    private static final String COOKIE_NAME = "refreshToken";

    private AuthenticationService authenticationService;
    private WebTestClient client;

    @BeforeEach
    void setUp() {
        authenticationService = mock(AuthenticationService.class);
        client = WebTestClient.bindToController(
                        new AuthenticationController(authenticationService))
                .controllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void loginWithoutTheDeliveryHeaderPutsTheRefreshTokenInTheBody() {
        when(authenticationService.login(any(), eq(TokenDelivery.BODY)))
                .thenReturn(Mono.just(new IssuedTokens("access.jwt", "opaque-refresh", 3600L)));

        client.post().uri("/api/members/login")
                .header("Content-Type", "application/json")
                .bodyValue(LOGIN_BODY)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist("Set-Cookie")
                .expectBody()
                .jsonPath("$.jwt").isEqualTo("access.jwt")
                .jsonPath("$.refreshToken").isEqualTo("opaque-refresh")
                .jsonPath("$.expiresIn").isEqualTo(3600);
    }

    @Test
    void loginInCookieModeKeepsTheRefreshTokenOutOfTheBody() {
        when(authenticationService.login(any(), eq(TokenDelivery.COOKIE)))
                .thenReturn(Mono.just(new IssuedTokens("access.jwt", "opaque-refresh", 3600L)));

        client.post().uri("/api/members/login")
                .header(TokenDelivery.HEADER, "cookie")
                .header("Content-Type", "application/json")
                .bodyValue(LOGIN_BODY)
                .exchange()
                .expectStatus().isOk()
                .expectCookie().value(COOKIE_NAME, value -> org.assertj.core.api.Assertions
                        .assertThat(value).isEqualTo("opaque-refresh"))
                .expectCookie().path(COOKIE_NAME, "/api/auth")
                .expectCookie().httpOnly(COOKIE_NAME, true)
                .expectCookie().secure(COOKIE_NAME, true)
                .expectCookie().sameSite(COOKIE_NAME, "Lax")
                .expectCookie().maxAge(COOKIE_NAME, Duration.ofDays(60))
                .expectBody()
                .jsonPath("$.jwt").isEqualTo("access.jwt")
                .jsonPath("$.expiresIn").isEqualTo(3600)
                .jsonPath("$.refreshToken").doesNotExist();
    }

    @Test
    void guestSignupKeepsThePasswordFieldAlongsideTheNewOnes() {
        when(authenticationService.joinGuest(any(), eq(TokenDelivery.BODY)))
                .thenReturn(Mono.just(new GuestTokens(
                        new IssuedTokens("access.jwt", "opaque-refresh", 3600L), "guest-pw")));

        client.post().uri("/api/members/guest")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.jwt").isEqualTo("access.jwt")
                .jsonPath("$.refreshToken").isEqualTo("opaque-refresh")
                .jsonPath("$.expiresIn").isEqualTo(3600)
                .jsonPath("$.password").isEqualTo("guest-pw");
    }

    @Test
    void refreshInCookieModeReadsTheCookieAndSetsTheSuccessor() {
        when(authenticationService.refresh(eq("opaque-refresh"), eq(TokenDelivery.COOKIE)))
                .thenReturn(Mono.just(new IssuedTokens("fresh.jwt", "next-refresh", 3600L)));

        client.post().uri("/api/auth/refresh")
                .header(TokenDelivery.HEADER, "cookie")
                .cookie(COOKIE_NAME, "opaque-refresh")
                .exchange()
                .expectStatus().isOk()
                .expectCookie().value(COOKIE_NAME, value -> org.assertj.core.api.Assertions
                        .assertThat(value).isEqualTo("next-refresh"))
                .expectBody()
                .jsonPath("$.jwt").isEqualTo("fresh.jwt")
                .jsonPath("$.refreshToken").doesNotExist();
    }

    @Test
    void refreshInBodyModeReadsTheBody() {
        when(authenticationService.refresh(eq("opaque-refresh"), eq(TokenDelivery.BODY)))
                .thenReturn(Mono.just(new IssuedTokens("fresh.jwt", "next-refresh", 3600L)));

        client.post().uri("/api/auth/refresh")
                .header("Content-Type", "application/json")
                .bodyValue("{\"refreshToken\":\"opaque-refresh\"}")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist("Set-Cookie")
                .expectBody()
                .jsonPath("$.refreshToken").isEqualTo("next-refresh");
    }

    @Test
    void aRejectedRefreshTokenAnswers401() {
        when(authenticationService.refresh(any(), any()))
                .thenReturn(Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "refresh token 이 유효하지 않습니다.")));

        client.post().uri("/api/auth/refresh")
                .header("Content-Type", "application/json")
                .bodyValue("{\"refreshToken\":\"stolen\"}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void logoutRevokesTheTokenAndAnswers204() {
        when(authenticationService.logout("opaque-refresh")).thenReturn(Mono.empty());

        client.post().uri("/api/auth/logout")
                .header("Content-Type", "application/json")
                .bodyValue("{\"refreshToken\":\"opaque-refresh\"}")
                .exchange()
                .expectStatus().isNoContent()
                .expectBody().isEmpty();

        verify(authenticationService).logout("opaque-refresh");
    }

    @Test
    void logoutInCookieModeClearsTheCookie() {
        when(authenticationService.logout("opaque-refresh")).thenReturn(Mono.empty());

        client.post().uri("/api/auth/logout")
                .header(TokenDelivery.HEADER, "cookie")
                .cookie(COOKIE_NAME, "opaque-refresh")
                .exchange()
                .expectStatus().isNoContent()
                .expectCookie().maxAge(COOKIE_NAME, Duration.ZERO)
                .expectCookie().path(COOKIE_NAME, "/api/auth");
    }
}
