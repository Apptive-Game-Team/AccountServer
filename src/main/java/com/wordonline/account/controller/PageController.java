package com.wordonline.account.controller;

import java.time.Duration;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

import com.wordonline.account.domain.IssuedTokens;
import com.wordonline.account.dto.LoginRequest;
import com.wordonline.account.dto.TokenDelivery;
import com.wordonline.account.service.AuthenticationService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Controller
public class PageController {
    private static final String REFRESH_COOKIE = "accountAdminRefreshToken";
    private static final String SESSION_PATH = "/login/session";
    private static final String SESSION_HEADER = "X-Console-Session=1";

    private final AuthenticationService authenticationService;

    @GetMapping("/login")
    public String login(@RequestParam(required = false) String signedOut,
                        @RequestParam(required = false) String error, Model model) {
        model.addAttribute("restoreSession", signedOut == null && error == null);
        model.addAttribute("loginError", error != null);
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
            return authenticationService.login(loginRequest, TokenDelivery.COOKIE)
                    .map(tokens -> {
                        writeSession(exchange, tokens);
                        return "redirect:/admin";
                    })
                    .onErrorReturn("redirect:/login?error");
        });
    }

    @PostMapping(path = SESSION_PATH + "/refresh", headers = SESSION_HEADER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> refresh(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            ServerWebExchange exchange) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        }
        return authenticationService.refresh(refreshToken, TokenDelivery.COOKIE)
                .doOnNext(tokens -> writeSession(exchange, tokens))
                .then();
    }

    @PostMapping(path = SESSION_PATH + "/logout", headers = SESSION_HEADER)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> logout(
            @CookieValue(name = REFRESH_COOKIE, required = false) String refreshToken,
            ServerWebExchange exchange) {
        return authenticationService.logout(refreshToken)
                .then(Mono.fromRunnable(() -> {
                    addCookie(exchange, "accessToken", "", "/", Duration.ZERO);
                    addCookie(exchange, REFRESH_COOKIE, "", SESSION_PATH, Duration.ZERO);
                }));
    }

    private static void writeSession(ServerWebExchange exchange, IssuedTokens tokens) {
        addCookie(exchange, "accessToken", tokens.accessToken(), "/", Duration.ofSeconds(tokens.expiresInSeconds()));
        addCookie(exchange, REFRESH_COOKIE, tokens.refreshToken(), SESSION_PATH, Duration.ofDays(60));
    }

    private static void addCookie(ServerWebExchange exchange, String name, String value,
                                  String path, Duration maxAge) {
        exchange.getResponse().addCookie(ResponseCookie.from(name, value)
                .path(path).secure(true).httpOnly(true).sameSite("Strict").maxAge(maxAge)
                .build());
    }
}
