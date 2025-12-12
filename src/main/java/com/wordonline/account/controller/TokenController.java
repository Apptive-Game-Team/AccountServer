package com.wordonline.account.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.thymeleaf.spring6.context.webflux.ReactiveDataDriverContextVariable;

import com.wordonline.account.dto.ServerTokenRequest;
import com.wordonline.account.dto.ServerTokenResponse;
import com.wordonline.account.service.AuthorityService;
import com.wordonline.account.service.TokenService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Arrays;
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
            ServerWebExchange exchange
    ) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    String expiryStr = formData.getFirst("expiryMinutes");
                    Long expiryMinutes = (expiryStr == null || expiryStr.isEmpty() || "infinite".equals(expiryStr)) 
                            ? null 
                            : Long.parseLong(expiryStr);
                    
                    List<String> authorityIdStrs = formData.get("authorityIds");
                    List<Long> authorityIds = (authorityIdStrs != null) 
                            ? authorityIdStrs.stream()
                                    .map(Long::parseLong)
                                    .collect(Collectors.toList())
                            : List.of();
                    
                    ServerTokenRequest request = new ServerTokenRequest(expiryMinutes, authorityIds);
                    return tokenService.generateServerToken(request);
                });
    }
}
