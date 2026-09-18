package com.wordonline.account.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;

class WebSecurityConfigTest {

    private static final String WEBGL_ORIGIN = "https://arcanecasters.theevilent.com";
    private static final List<String> ALLOWED_ORIGINS = List.of(
            WEBGL_ORIGIN, "https://account.theevilent.com", "http://localhost:[*]");

    private WebTestClient client;
    private WebSecurityConfig config;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        var keyPair = keyPairGenerator.generateKeyPair();

        config = new WebSecurityConfig(new CookieAuthenticationFilter());
        ReflectionTestUtils.setField(config, "allowedOrigins", ALLOWED_ORIGINS);
        ReflectionTestUtils.setField(config, "rsaPublicKey", (RSAPublicKey) keyPair.getPublic());
        ReflectionTestUtils.setField(config, "rsaPrivateKey", (RSAPrivateKey) keyPair.getPrivate());

        SecurityWebFilterChain chain = config.springSecurityFilterChain(
                ServerHttpSecurity.http(),
                config.jwtDecoder(),
                config.reactiveJwtAuthenticationConverterAdapter());

        client = WebTestClient.bindToWebHandler(exchange -> {
                    exchange.getResponse().setStatusCode(HttpStatus.OK);
                    return exchange.getResponse().setComplete();
                })
                .webFilter(new WebFilterChainProxy(chain))
                .build();
    }

    private String token(String scope) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject("tester")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(600))
                .claim("memberId", 1L)
                .claim("scope", scope)
                .build();
        return config.jwtEncoder(config.jwkSet())
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }

    @Test
    void memberLookupIsNotPubliclyReadable() {
        client.get().uri("/api/members/1").exchange().expectStatus().isFound();
    }

    @Test
    void authenticationEndpointsStayPublic() {
        client.post().uri("/api/members/guest").exchange().expectStatus().isOk();
        client.post().uri("/api/members/login").exchange().expectStatus().isOk();
        client.post().uri("/api/members").exchange().expectStatus().isOk();
    }

    @Test
    void refreshAndLogoutStayPublic() {
        // Both authenticate by refresh token; requiring an access token would defeat the point.
        client.post().uri("/api/auth/refresh").exchange().expectStatus().isOk();
        client.post().uri("/api/auth/logout").exchange().expectStatus().isOk();
    }

    // WebTestClient only fills in the request scheme when the URI is absolute, and the CORS
    // processor needs it to tell a cross-site request from a same-origin one.
    private static final String PREFLIGHT_URI = "http://localhost:8080/api/auth/refresh";

    @Test
    void credentialedRequestsFromAnUnlistedSiteArePrevented() {
        client.options().uri(PREFLIGHT_URI)
                .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectStatus().isForbidden()
                .expectHeader().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
    }

    @Test
    void theWebGlBuildKeepsCredentialedAccess() {
        client.options().uri(PREFLIGHT_URI)
                .header(HttpHeaders.ORIGIN, WEBGL_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, WEBGL_ORIGIN)
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
    }

    @Test
    void localhostKeepsCredentialedAccessOnAnyDevelopmentPort() {
        client.options().uri(PREFLIGHT_URI)
                .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:5173");
    }

    @Test
    void aWildcardOriginIsRefusedAtStartup() {
        WebSecurityConfig wildcard = new WebSecurityConfig(new CookieAuthenticationFilter());
        ReflectionTestUtils.setField(wildcard, "allowedOrigins", List.of("*"));

        assertThatThrownBy(wildcard::corsConfigurationSource)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(WebSecurityConfig.ALLOWED_ORIGINS_PROPERTY);
    }

    @Test
    void adminConsoleRejectsTokenWithoutSuperAdmin() {
        client.get().uri("/admin").headers(h -> h.setBearerAuth(token("")))
                .exchange().expectStatus().isForbidden();
        client.get().uri("/admin/members").headers(h -> h.setBearerAuth(token("")))
                .exchange().expectStatus().isForbidden();
        client.post().uri("/admin/systems").headers(h -> h.setBearerAuth(token("OTHER_ROLE")))
                .exchange().expectStatus().isForbidden();
    }

    @Test
    void adminConsoleAllowsSuperAdmin() {
        client.get().uri("/admin/members").headers(h -> h.setBearerAuth(token("SUPER_ADMIN")))
                .exchange().expectStatus().isOk();
    }
}
