package com.wordonline.account.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Cloudflare Turnstile settings for POST /api/members.
 *
 * @param secretKey Turnstile secret key; empty turns verification off entirely
 * @param required  whether a request without a captchaToken is rejected
 * @param verifyUrl Cloudflare siteverify endpoint
 * @param timeout   how long to wait for the siteverify answer
 */
@ConfigurationProperties(prefix = "turnstile")
public record TurnstileProperties(
        @DefaultValue("") String secretKey,
        @DefaultValue("false") boolean required,
        @DefaultValue("https://challenges.cloudflare.com/turnstile/v0/siteverify") String verifyUrl,
        @DefaultValue("5s") Duration timeout
) {

    public boolean enabled() {
        return secretKey != null && !secretKey.isBlank();
    }
}
