package com.wordonline.account.controller;

import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;

import com.wordonline.account.config.RefreshTokenCookie;
import com.wordonline.account.domain.GuestTokens;
import com.wordonline.account.domain.IssuedTokens;
import com.wordonline.account.dto.AuthResponse;
import com.wordonline.account.dto.GuestAuthResponse;
import com.wordonline.account.dto.GuestRequest;
import com.wordonline.account.dto.JoinRequest;
import com.wordonline.account.dto.LoginRequest;
import com.wordonline.account.dto.RefreshRequest;
import com.wordonline.account.dto.TokenDelivery;
import com.wordonline.account.service.AuthenticationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    @PostMapping("/members/guest")
    public Mono<GuestAuthResponse> createGuestMember(
            @RequestBody(required = false) GuestRequest guestRequest,
            @RequestHeader(value = TokenDelivery.HEADER, required = false) String deliveryHeader,
            ServerWebExchange exchange
    ) {
        String name = (guestRequest != null) ? guestRequest.name() : null;
        TokenDelivery delivery = TokenDelivery.fromHeader(deliveryHeader);
        return authenticationService.joinGuest(name, delivery)
                .map(guest -> toGuestResponse(guest, delivery, exchange));
    }

    @PostMapping("/members")
    public Mono<AuthResponse> createMember(
            @Validated @RequestBody JoinRequest joinRequest,
            @RequestHeader(value = TokenDelivery.HEADER, required = false) String deliveryHeader,
            ServerWebExchange exchange
    ) {
        TokenDelivery delivery = TokenDelivery.fromHeader(deliveryHeader);
        return authenticationService.join(joinRequest, delivery)
                .map(tokens -> toAuthResponse(tokens, delivery, exchange));
    }

    @PostMapping("/members/login")
    public Mono<AuthResponse> login(
            @Validated @RequestBody LoginRequest memberRequest,
            @RequestHeader(value = TokenDelivery.HEADER, required = false) String deliveryHeader,
            ServerWebExchange exchange
    ) {
        TokenDelivery delivery = TokenDelivery.fromHeader(deliveryHeader);
        return authenticationService.login(memberRequest, delivery)
                .map(tokens -> toAuthResponse(tokens, delivery, exchange));
    }

    /**
     * Authenticated by the refresh token alone, so it carries no access token and is reachable
     * without one.
     */
    @PostMapping("/auth/refresh")
    public Mono<AuthResponse> refresh(
            @RequestBody(required = false) RefreshRequest refreshRequest,
            @RequestHeader(value = TokenDelivery.HEADER, required = false) String deliveryHeader,
            ServerWebExchange exchange
    ) {
        TokenDelivery delivery = TokenDelivery.fromHeader(deliveryHeader);
        String presentedToken = presentedRefreshToken(delivery, refreshRequest, exchange);
        return authenticationService.refresh(presentedToken, delivery)
                .map(tokens -> toAuthResponse(tokens, delivery, exchange));
    }

    @PostMapping("/auth/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> logout(
            @RequestBody(required = false) RefreshRequest refreshRequest,
            @RequestHeader(value = TokenDelivery.HEADER, required = false) String deliveryHeader,
            ServerWebExchange exchange
    ) {
        TokenDelivery delivery = TokenDelivery.fromHeader(deliveryHeader);
        String presentedToken = presentedRefreshToken(delivery, refreshRequest, exchange);
        if (delivery == TokenDelivery.COOKIE) {
            exchange.getResponse().addCookie(RefreshTokenCookie.cleared());
        }
        return authenticationService.logout(presentedToken);
    }

    @GetMapping("/members/check")
    public Mono<String> check(
            @AuthenticationPrincipal Jwt principal
    ) {
        Long memberId = principal.getClaim("memberId");
        return Mono.just(
                String.format("memberId: %d, scope: %s", memberId, principal.getClaim("scope")));
    }

    private AuthResponse toAuthResponse(
            IssuedTokens tokens,
            TokenDelivery delivery,
            ServerWebExchange exchange
    ) {
        if (delivery == TokenDelivery.COOKIE) {
            exchange.getResponse().addCookie(RefreshTokenCookie.issued(tokens.refreshToken()));
        }
        return AuthResponse.of(tokens, delivery);
    }

    private GuestAuthResponse toGuestResponse(
            GuestTokens guest,
            TokenDelivery delivery,
            ServerWebExchange exchange
    ) {
        if (delivery == TokenDelivery.COOKIE) {
            exchange.getResponse()
                    .addCookie(RefreshTokenCookie.issued(guest.tokens().refreshToken()));
        }
        return GuestAuthResponse.of(guest, delivery);
    }

    private String presentedRefreshToken(
            TokenDelivery delivery,
            RefreshRequest refreshRequest,
            ServerWebExchange exchange
    ) {
        if (delivery == TokenDelivery.COOKIE) {
            HttpCookie cookie = exchange.getRequest().getCookies()
                    .getFirst(RefreshTokenCookie.NAME);
            return cookie == null ? null : cookie.getValue();
        }
        return refreshRequest == null ? null : refreshRequest.refreshToken();
    }
}
