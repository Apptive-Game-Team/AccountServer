package com.wordonline.account.domain;

/**
 * The successor issued when a refresh token is spent, and the member it belongs to.
 */
public record RotatedRefreshToken(
        long memberId,
        String refreshToken
) {

}
