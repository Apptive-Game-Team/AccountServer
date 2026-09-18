package com.wordonline.account.config;

import java.time.Duration;

import org.springframework.http.ResponseCookie;

/**
 * The {@code Set-Cookie} used by the WebGL build, which cannot keep a refresh token out of
 * reach of page scripts any other way.
 */
public final class RefreshTokenCookie {

    public static final String NAME = "refreshToken";

    /**
     * {@code /api/auth}, not {@code /api/auth/refresh}: logout has to receive the cookie too,
     * while the other endpoints of this server never do.
     */
    static final String PATH = "/api/auth";

    static final Duration LIFETIME = Duration.ofDays(60);

    private RefreshTokenCookie() {
    }

    public static ResponseCookie issued(String refreshToken) {
        return build(refreshToken, LIFETIME);
    }

    public static ResponseCookie cleared() {
        return build("", Duration.ZERO);
    }

    private static ResponseCookie build(String value, Duration maxAge) {
        return ResponseCookie.from(NAME, value)
                .path(PATH)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .maxAge(maxAge)
                .build();
    }
}
