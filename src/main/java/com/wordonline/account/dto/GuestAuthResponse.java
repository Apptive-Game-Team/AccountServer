package com.wordonline.account.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.wordonline.account.domain.GuestTokens;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record GuestAuthResponse(
        String jwt,
        String refreshToken,
        long expiresIn,
        String password
) {

    public static GuestAuthResponse of(GuestTokens guest, TokenDelivery delivery) {
        AuthResponse authResponse = AuthResponse.of(guest.tokens(), delivery);
        return new GuestAuthResponse(
                authResponse.jwt(),
                authResponse.refreshToken(),
                authResponse.expiresIn(),
                guest.guestPassword());
    }
}
