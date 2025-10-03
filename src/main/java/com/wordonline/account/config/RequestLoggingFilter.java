package com.wordonline.account.config;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import java.util.Objects;

@Slf4j
@Component
public class RequestLoggingFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long startTime = System.currentTimeMillis();
        ServerHttpRequest request = exchange.getRequest();

        String clientIp = request.getHeaders().getFirst("X-Forwarded-For");
        if (clientIp == null) {
            clientIp = request.getRemoteAddress() != null ? request.getRemoteAddress().getAddress().getHostAddress() : "Unknown";
        }

        log.info("[Request Start] method={}, uri={}, ip={}, userAgent={}",
                request.getMethod(),
                request.getURI(),
                clientIp,
                Objects.requireNonNullElse(request.getHeaders().getFirst(HttpHeaders.USER_AGENT), "Unknown")
        );

        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    log.info("[Request End] method={}, uri={}, duration={}ms",
                            request.getMethod(),
                            request.getURI(),
                            duration
                    );
                });
    }
}
