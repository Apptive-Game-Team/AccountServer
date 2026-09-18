package com.wordonline.account.config;

import java.util.List;

import org.springframework.http.HttpCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

/**
 * Lets the Thymeleaf admin pages authenticate with the {@code accessToken} cookie the login
 * form sets, by rewriting it into the Bearer header the resource server reads.
 *
 * <p>Scoped to those pages. On the API surface a cookie must never stand in for an
 * Authorization header: browsers attach cookies to cross-site requests, so an API that accepted
 * one would be acting on a request the caller's own site did not make.
 */
@Component
public class CookieAuthenticationFilter implements WebFilter {

    private static final String ACCESS_TOKEN_COOKIE = "accessToken";
    private static final List<String> COOKIE_AUTHENTICATED_PATHS = List.of("/admin", "/login");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        if (!isCookieAuthenticated(exchange.getRequest().getPath().value())) {
            return chain.filter(exchange);
        }

        HttpCookie accessTokenCookie = exchange.getRequest().getCookies()
                .getFirst(ACCESS_TOKEN_COOKIE);

        if (accessTokenCookie != null) {
            String token = accessTokenCookie.getValue();
            ServerWebExchange modifiedExchange = exchange.mutate()
                    .request(r -> r.headers(headers -> {
                        headers.setBearerAuth(token);
                    }))
                    .build();
            return chain.filter(modifiedExchange);
        }

        return chain.filter(exchange);
    }

    private static boolean isCookieAuthenticated(String path) {
        return COOKIE_AUTHENTICATED_PATHS.stream()
                .anyMatch(prefix -> path.equals(prefix) || path.startsWith(prefix + "/"));
    }
}
