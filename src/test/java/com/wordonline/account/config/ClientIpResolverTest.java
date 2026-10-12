package com.wordonline.account.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.time.Duration;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;

class ClientIpResolverTest {

    private final ClientIpResolver resolver = new ClientIpResolver(
            new SignupRateLimitProperties(5, 20, Duration.ofHours(1), 100, "X-Real-IP"));

    private static ServerHttpRequest request(String realIp, String forwardedFor) throws Exception {
        MockServerHttpRequest.BaseBuilder<?> builder = MockServerHttpRequest.post("/api/members")
                .remoteAddress(new InetSocketAddress(InetAddress.getByName("172.18.0.2"), 4000));
        if (realIp != null) {
            builder.header("X-Real-IP", realIp);
        }
        if (forwardedFor != null) {
            builder.header("X-Forwarded-For", forwardedFor);
        }
        return builder.build();
    }

    @Test
    void realIpHeaderIsUsedWhenPresent() throws Exception {
        assertEquals("203.0.113.9", resolver.resolve(request("203.0.113.9", null)));
        assertEquals("2001:db8:0:0:0:0:0:1", resolver.resolve(request("2001:db8::1", null)));
    }

    @Test
    void spoofedForwardedForIsIgnored() throws Exception {
        assertEquals("203.0.113.9",
                resolver.resolve(request("203.0.113.9", "1.1.1.1, 203.0.113.9")));
        assertEquals("172.18.0.2", resolver.resolve(request(null, "1.1.1.1")));
    }

    @Test
    void remoteAddressIsUsedWhenRealIpIsAbsentOrMalformed() throws Exception {
        assertEquals("172.18.0.2", resolver.resolve(request(null, null)));
        assertEquals("172.18.0.2", resolver.resolve(request("not-an-ip", null)));
        assertEquals("172.18.0.2", resolver.resolve(request("999.1.1.1.1", null)));
        assertEquals("172.18.0.2", resolver.resolve(request("deadbeef", null)));
        assertEquals("172.18.0.2", resolver.resolve(request("", null)));
    }
}
