package com.wordonline.account.dto;

import java.util.Locale;

/**
 * How a caller wants the refresh token handed over, selected by the {@code X-Token-Delivery}
 * request header.
 */
public enum TokenDelivery {

    /** Refresh token in the response body. The client stores it in OS storage. */
    BODY,

    /** Refresh token only in a {@code Set-Cookie}. Used by the WebGL build. */
    COOKIE;

    public static final String HEADER = "X-Token-Delivery";

    private static final String COOKIE_HEADER_VALUE = "cookie";

    /**
     * An absent or unrecognised header means body, so every caller written before refresh
     * tokens existed keeps the behaviour it already has.
     */
    public static TokenDelivery fromHeader(String headerValue) {
        if (headerValue == null) {
            return BODY;
        }
        return COOKIE_HEADER_VALUE.equalsIgnoreCase(headerValue.trim()) ? COOKIE : BODY;
    }

    /**
     * Stored on the token row. The delivery mode is the only signal the contract carries about
     * the caller: cookie is the WebGL build, body is Android, macOS and Windows.
     */
    public String platform() {
        return name().toLowerCase(Locale.ROOT);
    }
}
