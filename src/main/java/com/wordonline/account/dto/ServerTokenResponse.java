package com.wordonline.account.dto;

public record ServerTokenResponse(
        String token,
        String expiryInfo
) {
}
