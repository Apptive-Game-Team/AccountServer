package com.wordonline.account.domain;

/**
 * A guest signup. The generated password is returned once, because nothing else can recover the
 * account afterwards.
 */
public record GuestTokens(
        IssuedTokens tokens,
        String guestPassword
) {

}
