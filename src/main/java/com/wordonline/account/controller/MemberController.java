package com.wordonline.account.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wordonline.account.dto.MemberResponse;
import com.wordonline.account.service.MemberService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/members")
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public Mono<MemberResponse> getMember(
            @AuthenticationPrincipal Jwt principal
    ) {
        long memberId = principal.getClaim("memberId");
        return memberService.getMember(memberId)
                .map(MemberResponse::new);
    }
}
