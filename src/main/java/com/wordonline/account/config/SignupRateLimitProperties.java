package com.wordonline.account.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Per-IP limits on the sign-up endpoints. Counters live in memory, so every limit applies per
 * server instance.
 *
 * @param members    requests per window for POST /api/members
 * @param guest      requests per window for POST /api/members/guest
 * @param window     length of the fixed window
 * @param maxEntries most IP addresses tracked per endpoint at once
 */
@ConfigurationProperties(prefix = "signup-rate-limit")
public record SignupRateLimitProperties(
        @DefaultValue("5") int members,
        @DefaultValue("20") int guest,
        @DefaultValue("1h") Duration window,
        @DefaultValue("10000") int maxEntries
) {

}
