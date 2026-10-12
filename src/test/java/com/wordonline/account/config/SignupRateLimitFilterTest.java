package com.wordonline.account.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

class SignupRateLimitFilterTest {

    private static final class MutableClock extends Clock {

        private Instant now = Instant.parse("2026-10-11T00:00:00Z");

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private static final SignupRateLimitProperties PROPERTIES =
            new SignupRateLimitProperties(2, 3, Duration.ofHours(1), 100, "X-Real-IP");

    private MutableClock clock;
    private AtomicInteger passed;
    private WebFilterChain chain;
    private SignupRateLimitFilter filter;

    @BeforeEach
    void setUp() {
        clock = new MutableClock();
        passed = new AtomicInteger();
        chain = exchange -> {
            passed.incrementAndGet();
            return Mono.empty();
        };
        filter = new SignupRateLimitFilter(
                PROPERTIES, clock, new ClientIpResolver(PROPERTIES));
    }

    private MockServerWebExchange call(HttpMethod method, String path, String ip)
            throws Exception {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(method, path)
                        .remoteAddress(new InetSocketAddress(InetAddress.getByName(ip), 4000))
                        .build());
        filter.filter(exchange, chain).block();
        return exchange;
    }

    @Test
    void membersEndpointAllowsTheLimitThenAnswers429WithRetryAfter() throws Exception {
        call(HttpMethod.POST, "/api/members", "203.0.113.1");
        call(HttpMethod.POST, "/api/members", "203.0.113.1");
        assertEquals(2, passed.get());

        clock.advance(Duration.ofMinutes(10));
        MockServerWebExchange limited = call(HttpMethod.POST, "/api/members", "203.0.113.1");

        assertEquals(2, passed.get());
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, limited.getResponse().getStatusCode());
        assertEquals("3000", limited.getResponse().getHeaders().getFirst("Retry-After"));
        assertTrue(limited.getResponse().getBodyAsString().block().contains("너무 많습니다"));
    }

    @Test
    void endpointsKeepSeparateBuckets() throws Exception {
        call(HttpMethod.POST, "/api/members", "203.0.113.1");
        call(HttpMethod.POST, "/api/members", "203.0.113.1");
        MockServerWebExchange members = call(HttpMethod.POST, "/api/members", "203.0.113.1");
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, members.getResponse().getStatusCode());

        for (int i = 0; i < 3; i++) {
            MockServerWebExchange guest = call(HttpMethod.POST, "/api/members/guest",
                    "203.0.113.1");
            assertNull(guest.getResponse().getStatusCode());
        }
        MockServerWebExchange guestLimited = call(HttpMethod.POST, "/api/members/guest",
                "203.0.113.1");
        assertEquals(HttpStatus.TOO_MANY_REQUESTS, guestLimited.getResponse().getStatusCode());
    }

    @Test
    void addressesKeepSeparateBuckets() throws Exception {
        call(HttpMethod.POST, "/api/members", "203.0.113.1");
        call(HttpMethod.POST, "/api/members", "203.0.113.1");
        call(HttpMethod.POST, "/api/members", "203.0.113.1");

        MockServerWebExchange other = call(HttpMethod.POST, "/api/members", "203.0.113.2");

        assertNull(other.getResponse().getStatusCode());
    }

    @Test
    void windowResetsAfterItsLength() throws Exception {
        for (int i = 0; i < 3; i++) {
            call(HttpMethod.POST, "/api/members", "203.0.113.1");
        }
        clock.advance(Duration.ofHours(1));

        MockServerWebExchange again = call(HttpMethod.POST, "/api/members", "203.0.113.1");

        assertNull(again.getResponse().getStatusCode());
    }

    @Test
    void otherRequestsAreNotCounted() throws Exception {
        for (int i = 0; i < 10; i++) {
            call(HttpMethod.GET, "/api/members", "203.0.113.1");
            call(HttpMethod.POST, "/api/members/login", "203.0.113.1");
            call(HttpMethod.OPTIONS, "/api/members", "203.0.113.1");
        }
        assertEquals(30, passed.get());
    }

    @Test
    void trackedAddressesStayWithinTheCap() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(1, Duration.ofHours(1), 3,
                clock);
        for (int i = 0; i < 10; i++) {
            limiter.tryAcquire("ip-" + i);
        }
        assertEquals(3, limiter.size());
    }

    @Test
    void expiredAddressesAreEvicted() {
        FixedWindowRateLimiter limiter = new FixedWindowRateLimiter(1, Duration.ofHours(1), 100,
                clock);
        limiter.tryAcquire("a");
        limiter.tryAcquire("b");
        clock.advance(Duration.ofHours(2));
        assertFalse(limiter.tryAcquire("c").retryAfterSeconds() > 0);
        assertEquals(1, limiter.size());
    }
}
