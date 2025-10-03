package com.wordonline.account.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.wordonline.account.dto.ValueRequest;
import com.wordonline.account.dto.ValueResponse;
import com.wordonline.account.service.KeyValueService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@PreAuthorize("isAuthenticated()")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class KeyValueController {

    private final KeyValueService keyValueService;

    @GetMapping("/systems/{systemId}/key-values/{key}")
    public Mono<ValueResponse> getValue(
            @PathVariable Long systemId,
            @PathVariable String key
    ) {
        return keyValueService.getSystemValue(systemId, key);
    }

    @GetMapping("/members/me/key-values/{key}")
    public Mono<ValueResponse> getMyValue(
            @AuthenticationPrincipal Jwt principal,
            @PathVariable String key
    ) {
        return keyValueService.getMemberValue(principal.getClaim("memberId"), key);
    }

    @PutMapping("/members/me/key-values/{key}")
    public Mono<ValueResponse> setMyValue(
            @AuthenticationPrincipal Jwt principal,
            @PathVariable String key,
            @RequestBody ValueRequest valueRequest
    ) {
        return keyValueService.setMemberValue(principal.getClaim("memberId"), key, valueRequest.value());
    }

}
