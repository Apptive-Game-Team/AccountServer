package com.wordonline.account.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.wordonline.account.config.JwtProvider;
import com.wordonline.account.domain.Authority;
import com.wordonline.account.dto.ServerTokenRequest;
import com.wordonline.account.dto.ServerTokenResponse;
import com.wordonline.account.entity.System;

import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private AuthorityService authorityService;

    private TokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new TokenService(jwtProvider, authorityService);
    }

    @Test
    void testGenerateServerTokenWithExpiry() {
        // Arrange
        Long expiryMinutes = 60L;
        List<Long> authorityIds = List.of(1L, 2L);
        
        System system = new System(1L, 1L, "TEST_SYSTEM", 1L);
        Authority authority1 = new Authority(1L, system, "READ");
        Authority authority2 = new Authority(2L, system, "WRITE");
        
        when(authorityService.getAuthoritiesByIds(authorityIds))
                .thenReturn(Flux.just(authority1, authority2));
        
        when(jwtProvider.generateServerToken(anyString(), anyLong()))
                .thenReturn("test.jwt.token");
        
        ServerTokenRequest request = new ServerTokenRequest(expiryMinutes, authorityIds);
        
        // Act & Assert
        StepVerifier.create(tokenService.generateServerToken(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("test.jwt.token", response.token());
                    assertEquals("Expires in 60 minutes", response.expiryInfo());
                })
                .verifyComplete();
    }

    @Test
    void testGenerateServerTokenWithInfiniteExpiry() {
        // Arrange
        List<Long> authorityIds = List.of(1L);
        
        System system = new System(1L, 1L, "TEST_SYSTEM", 1L);
        Authority authority = new Authority(1L, system, "ADMIN");
        
        when(authorityService.getAuthoritiesByIds(authorityIds))
                .thenReturn(Flux.just(authority));
        
        when(jwtProvider.generateServerToken("TESTSYSTEM_ADMIN", null))
                .thenReturn("test.jwt.token.infinite");
        
        ServerTokenRequest request = new ServerTokenRequest(null, authorityIds);
        
        // Act & Assert
        StepVerifier.create(tokenService.generateServerToken(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("test.jwt.token.infinite", response.token());
                    assertEquals("Never expires", response.expiryInfo());
                })
                .verifyComplete();
    }

    @Test
    void testGenerateServerTokenWithNoAuthorities() {
        // Arrange
        List<Long> authorityIds = List.of();
        
        when(authorityService.getAuthoritiesByIds(authorityIds))
                .thenReturn(Flux.empty());
        
        when(jwtProvider.generateServerToken("", 30L))
                .thenReturn("test.jwt.token.empty");
        
        ServerTokenRequest request = new ServerTokenRequest(30L, authorityIds);
        
        // Act & Assert
        StepVerifier.create(tokenService.generateServerToken(request))
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals("test.jwt.token.empty", response.token());
                    assertEquals("Expires in 30 minutes", response.expiryInfo());
                })
                .verifyComplete();
    }
}
