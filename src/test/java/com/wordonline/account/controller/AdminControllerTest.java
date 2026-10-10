package com.wordonline.account.controller;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ServerWebExchange;

import com.wordonline.account.dto.AuthorityResponse;
import com.wordonline.account.entity.System;
import com.wordonline.account.service.AuthorityService;
import com.wordonline.account.service.MemberService;
import com.wordonline.account.service.SystemService;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private SystemService systemService;

    @Mock
    private MemberService memberService;

    @Mock
    private AuthorityService authorityService;

    private AdminController adminController;

    @BeforeEach
    void setUp() {
        adminController = new AdminController(systemService, memberService, authorityService);
    }

    @Test
    void createSystem_Success() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("memberId")).thenReturn(100L);

        ServerWebExchange exchange = MockServerWebExchange.builder(
                MockServerHttpRequest.post("/admin/systems")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .body("name=TestSystem"))
                .build();

        System system = new System(1L, "TestSystem", 100L);
        when(systemService.createSystem(100L, "TestSystem")).thenReturn(Mono.just(system));

        Mono<String> result = adminController.createSystem(exchange, jwt);

        StepVerifier.create(result)
                .expectNext("redirect:/admin/systems")
                .verifyComplete();
    }

    @Test
    void createSystem_ErrorPropagation() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getClaim("memberId")).thenReturn(100L);

        ServerWebExchange exchange = MockServerWebExchange.builder(
                MockServerHttpRequest.post("/admin/systems")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .body("name=TestSystem"))
                .build();

        when(systemService.createSystem(100L, "TestSystem"))
                .thenReturn(Mono.error(new RuntimeException("Database error")));

        Mono<String> result = adminController.createSystem(exchange, jwt);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void updateSystem_Success() {
        ServerWebExchange exchange = MockServerWebExchange.builder(
                MockServerHttpRequest.post("/admin/systems/1")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .body("name=UpdatedSystem"))
                .build();

        System system = new System(1L, "UpdatedSystem", 100L);
        when(systemService.updateSystem(1L, "UpdatedSystem")).thenReturn(Mono.just(system));

        Mono<String> result = adminController.updateSystem(1L, exchange);

        StepVerifier.create(result)
                .expectNext("redirect:/admin/systems")
                .verifyComplete();
    }

    @Test
    void updateSystem_ErrorPropagation() {
        ServerWebExchange exchange = MockServerWebExchange.builder(
                MockServerHttpRequest.post("/admin/systems/1")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .body("name=UpdatedSystem"))
                .build();

        when(systemService.updateSystem(1L, "UpdatedSystem"))
                .thenReturn(Mono.error(new RuntimeException("System not found")));

        Mono<String> result = adminController.updateSystem(1L, exchange);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void createAuthority_Success() {
        ServerWebExchange exchange = MockServerWebExchange.builder(
                MockServerHttpRequest.post("/admin/authorities")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .body("name=ROLE_ADMIN&systemId=10"))
                .build();

        AuthorityResponse response = mock(AuthorityResponse.class);
        when(authorityService.createAuthority(10L, "ROLE_ADMIN")).thenReturn(Mono.just(response));

        Mono<String> result = adminController.createAuthority(exchange);

        StepVerifier.create(result)
                .expectNext("redirect:/admin/authorities")
                .verifyComplete();
    }

    @Test
    void createAuthority_ErrorPropagation() {
        ServerWebExchange exchange = MockServerWebExchange.builder(
                MockServerHttpRequest.post("/admin/authorities")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .body("name=ROLE_ADMIN&systemId=10"))
                .build();

        when(authorityService.createAuthority(10L, "ROLE_ADMIN"))
                .thenReturn(Mono.error(new RuntimeException("Authority already exists")));

        Mono<String> result = adminController.createAuthority(exchange);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }

    @Test
    void updateAuthority_Success() {
        ServerWebExchange exchange = MockServerWebExchange.builder(
                MockServerHttpRequest.post("/admin/authorities/5")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .body("name=ROLE_SUPERADMIN"))
                .build();

        AuthorityResponse response = mock(AuthorityResponse.class);
        when(authorityService.updateAuthority(5L, "ROLE_SUPERADMIN")).thenReturn(Mono.just(response));

        Mono<String> result = adminController.updateAuthority(5L, exchange);

        StepVerifier.create(result)
                .expectNext("redirect:/admin/authorities")
                .verifyComplete();
    }

    @Test
    void updateAuthority_ErrorPropagation() {
        ServerWebExchange exchange = MockServerWebExchange.builder(
                MockServerHttpRequest.post("/admin/authorities/5")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .body("name=ROLE_SUPERADMIN"))
                .build();

        when(authorityService.updateAuthority(5L, "ROLE_SUPERADMIN"))
                .thenReturn(Mono.error(new RuntimeException("Authority not found")));

        Mono<String> result = adminController.updateAuthority(5L, exchange);

        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();
    }
}
