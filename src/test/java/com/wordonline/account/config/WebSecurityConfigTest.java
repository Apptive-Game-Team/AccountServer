package com.wordonline.account.config;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterChainProxy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;

class WebSecurityConfigTest {

    private WebTestClient client;
    private WebSecurityConfig config;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        var keyPair = keyPairGenerator.generateKeyPair();

        config = new WebSecurityConfig(new CookieAuthenticationFilter());
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
