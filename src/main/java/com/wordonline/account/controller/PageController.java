package com.wordonline.account.controller;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ServerWebExchange;

import com.wordonline.account.dto.LoginRequest;
import com.wordonline.account.service.AuthenticationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Slf4j
@Controller
public class PageController {

    private final AuthenticationService authenticationService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/join")
    public String join() {
        return "join";
    }

    @PostMapping("/login")
    public Mono<String> handleLogin(ServerWebExchange exchange) {
        return exchange.getFormData().flatMap(formData -> {
            LoginRequest loginRequest = new LoginRequest(formData.getFirst("email"),
                    formData.getFirst("password"));
            return authenticationService.login(loginRequest)
                    .map(authResponse -> {
                        ResponseCookie cookie = ResponseCookie.from("accessToken",
                                        authResponse.jwt())
                                .httpOnly(true)
                                .path("/")
                                .maxAge(3600) // 1 hour
                                .secure(true) // Should be true in production
                                .build();
                        exchange.getResponse().addCookie(cookie);
                        return "redirect:/admin";
                    })
                    .onErrorResume(e -> {
                        log.error("[LOGIN FAIL]", e);
                        return Mono.just("redirect:/login?error");
                    });
        });
    }
}
