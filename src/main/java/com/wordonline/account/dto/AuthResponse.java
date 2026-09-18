package com.wordonline.account.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wordonline.account.domain.IssuedTokens;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuthResponse(
        String jwt,
        String refreshToken,
        long expiresIn
) {

    /**
     * In cookie mode the refresh token travels only in {@code Set-Cookie}, so the field is left
     * out of the body entirely rather than sent empty.
     */
    public static AuthResponse of(IssuedTokens tokens, TokenDelivery delivery) {
        return new AuthResponse(
                tokens.accessToken(),
                delivery == TokenDelivery.COOKIE ? null : tokens.refreshToken(),
                tokens.expiresInSeconds());
    }
}
