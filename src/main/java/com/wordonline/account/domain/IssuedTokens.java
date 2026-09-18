package com.wordonline.account.domain;

/**
 * One access token and the refresh token that can replace it. How the refresh token reaches the
 * caller is decided at the edge, not here.
 */
public record IssuedTokens(
        String accessToken,
        String refreshToken,
        long expiresInSeconds
) {

}
