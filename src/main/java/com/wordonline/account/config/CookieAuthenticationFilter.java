package com.wordonline.account.config;

import org.springframework.http.HttpCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import reactor.core.publisher.Mono;

@Component
public class CookieAuthenticationFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        HttpCookie accessTokenCookie = exchange.getRequest().getCookies().getFirst("accessToken");

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
}
