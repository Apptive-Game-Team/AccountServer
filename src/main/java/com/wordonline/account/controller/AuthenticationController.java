package com.wordonline.account.controller;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wordonline.account.dto.AuthResponse;
import com.wordonline.account.dto.MemberRequest;
import com.wordonline.account.service.AuthenticationService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/members")
    public Mono<AuthResponse> createMember(
        @Validated @RequestBody MemberRequest memberRequest
    ) {
        return authenticationService.join(memberRequest);
    }

    @PostMapping("/members/login")
    public Mono<AuthResponse> login(
            @Validated @RequestBody MemberRequest memberRequest
    ) {
        return authenticationService.login(memberRequest);
    }
}
