package com.wordonline.account.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;

import com.wordonline.account.dto.ServerTokenRequest;
import com.wordonline.account.dto.ServerTokenResponse;
import com.wordonline.account.service.AuthorityService;
import com.wordonline.account.service.TokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Controller
@PreAuthorize("hasAuthority('SUPER_ADMIN')")
@RequestMapping("/admin/tokens")
public class TokenController {

    private final TokenService tokenService;
    private final AuthorityService authorityService;

    @GetMapping
    public Mono<String> tokenPage(Model model) {
        var authorities = authorityService.getAuthorities(0, 1000);
        var authoritiesIReactive = new ReactiveDataDriverContextVariable(authorities, 1);
        model.addAttribute("authorities", authoritiesIReactive);
        
        return Mono.just("admin/tokens");
    }

    @PostMapping
    @ResponseBody
    public Mono<ServerTokenResponse> generateToken(
            @RequestBody ServerTokenRequest serverTokenRequest
    ) {
        return tokenService.generateServerToken(serverTokenRequest);
    }
}
