package com.wordonline.account.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wordonline.account.dto.AuthResponse;
import com.wordonline.account.dto.JoinRequest;
import com.wordonline.account.dto.LoginRequest;
import com.wordonline.account.service.AuthenticationService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/members/guest")
    public Mono<AuthResponse> createGuestMember(
            @RequestParam(required = false) String name
    ) {
        return authenticationService.joinGuest(name);
    }

    @PostMapping("/members")
    public Mono<AuthResponse> createMember(
        @Validated @RequestBody JoinRequest joinRequest
    ) {
        return authenticationService.join(joinRequest);
    }

    @PostMapping("/members/login")
    public Mono<AuthResponse> login(
            @Validated @RequestBody LoginRequest memberRequest
    ) {
        return authenticationService.login(memberRequest);
    }

    @GetMapping("/members/check")
    public Mono<String> check(
            @AuthenticationPrincipal Jwt principal
    ) {
        Long memberId = principal.getClaim("memberId");
        return Mono.just(String.format("memberId: %d, scope: %s", memberId, principal.getClaim("scope")));
    }
}
