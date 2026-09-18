package com.wordonline.account.config;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Mono;

class CookieAuthenticationFilterTest {

    private static final String ACCESS_TOKEN = "admin-session-token";

    // The handler echoes whatever Authorization header reached it, or "none".
    private final WebTestClient client = WebTestClient.bindToWebHandler(exchange -> {
                String authorization = exchange.getRequest().getHeaders()
                        .getFirst(HttpHeaders.AUTHORIZATION);
                exchange.getResponse().setStatusCode(HttpStatus.OK);
                byte[] body = (authorization == null ? "none" : authorization)
                        .getBytes(StandardCharsets.UTF_8);
                return exchange.getResponse().writeWith(
                        Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
            })
            .webFilter(new CookieAuthenticationFilter())
            .build();

    @Test
    void theAdminPagesAuthenticateWithTheCookie() {
        expectAuthorization("/admin", "Bearer " + ACCESS_TOKEN);
        expectAuthorization("/admin/members", "Bearer " + ACCESS_TOKEN);
        expectAuthorization("/login", "Bearer " + ACCESS_TOKEN);
    }

    @Test
    void theApiIgnoresTheCookie() {
        expectAuthorization("/api/members/check", "none");
        expectAuthorization("/api/members/1", "none");
        expectAuthorization("/api/auth/refresh", "none");
    }

    /** A path that merely starts with the same letters is not an admin page. */
    @Test
    void aLookAlikePathIgnoresTheCookie() {
        expectAuthorization("/administration", "none");
        expectAuthorization("/logins", "none");
    }

    private void expectAuthorization(String path, String expected) {
        client.get().uri(path)
                .cookie("accessToken", ACCESS_TOKEN)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class).isEqualTo(expected);
    }
}
