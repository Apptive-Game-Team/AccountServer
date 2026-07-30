package com.wordonline.account.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import com.wordonline.account.config.JwtProvider;
import com.wordonline.account.dto.JoinRequest;
import com.wordonline.account.dto.LoginRequest;
import com.wordonline.account.util.NicknameGenerator;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private MemberService memberService;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private NicknameGenerator nicknameGenerator;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(passwordEncoder, memberService,
                jwtProvider, nicknameGenerator);
    }

    @Test
    void loginWithUnknownEmailFailsWithUnauthorized() {
        when(memberService.getMember("nobody@example.com")).thenReturn(Mono.empty());

        StepVerifier.create(
                        authenticationService.login(
                                new LoginRequest("nobody@example.com", "12345678")))
                .expectErrorSatisfies(error -> {
                    ResponseStatusException statusException = assertInstanceOf(
                            ResponseStatusException.class, error);
                    assertEquals(HttpStatus.UNAUTHORIZED, statusException.getStatusCode());
                })
                .verify();
    }

    @Test
    void guestPasswordIsRandomAndNotDerivedFromEmail() {
        JoinRequest first = authenticationService.getRandomJoinRequest("guest").block();
        JoinRequest second = authenticationService.getRandomJoinRequest("guest").block();

        assertNotNull(first);
        assertNotNull(second);
        assertTrue(first.password().length() >= 8, "guest password must satisfy the 8 char minimum");
        assertNotEquals(first.password(), second.password());
        assertFalse(first.email().contains(first.password()),
                "guest email must not leak the password");
        // email carries no clock reading the password could be rebuilt from
        assertTrue(first.email().matches(
                "guest_[0-9a-f-]{36}@example\\.com"), first.email());
    }
}
