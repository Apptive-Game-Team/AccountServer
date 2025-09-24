package com.wordonline.account.config;

import java.nio.charset.StandardCharsets;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private static final String TOKEN_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    private String resolveToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (bearerToken != null && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        String token = resolveToken(exchange.getRequest());

        if (token != null) {
            if (jwtUtil.validateToken(token)) {
                Authentication authentication = jwtUtil.getAuthentication(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // Not Valid Token
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.UNAUTHORIZED);

                DataBuffer buffer = response.bufferFactory()
                        .wrap("Invalid JWT token".getBytes(StandardCharsets.UTF_8));
                return response.writeWith(Mono.just(buffer));
            }
        }

        return chain.filter(exchange);
    }
}
