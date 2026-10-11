package com.wordonline.account.config;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Limits how often one IP address may sign up, separately for POST /api/members and
 * POST /api/members/guest.
 *
 * <p>Ordered after the security filter chain (-100) so a 429 still carries the CORS headers the
 * website needs to read it. The address is the request's remote address, which is the
 * X-Forwarded-For client once server.forward-headers-strategy is on.
 */
@Slf4j
@Component
@Order(0)
public class SignupRateLimitFilter implements WebFilter {

    static final String MEMBERS_PATH = "/api/members";
    static final String GUEST_PATH = "/api/members/guest";
    static final String LIMITED_MESSAGE = "가입 요청이 너무 많습니다. 잠시 후 다시 시도해 주세요.";

    private final FixedWindowRateLimiter membersLimiter;
    private final FixedWindowRateLimiter guestLimiter;

    public SignupRateLimitFilter(SignupRateLimitProperties properties, Clock clock) {
        this.membersLimiter = new FixedWindowRateLimiter(properties.members(),
                properties.window(), properties.maxEntries(), clock);
        this.guestLimiter = new FixedWindowRateLimiter(properties.guest(),
                properties.window(), properties.maxEntries(), clock);
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        if (request.getMethod() != HttpMethod.POST) {
            return chain.filter(exchange);
        }
        String path = request.getPath().value();
        FixedWindowRateLimiter limiter = switch (path) {
            case MEMBERS_PATH -> membersLimiter;
            case GUEST_PATH -> guestLimiter;
            default -> null;
        };
        if (limiter == null) {
            return chain.filter(exchange);
        }

        String ip = clientIp(request);
        FixedWindowRateLimiter.Decision decision = limiter.tryAcquire(ip);
        if (decision.allowed()) {
            return chain.filter(exchange);
        }

        log.warn("[Signup Rate Limit] path={}, ip={}, retryAfter={}s", path, ip,
                decision.retryAfterSeconds());
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        response.getHeaders().set(HttpHeaders.RETRY_AFTER,
                Long.toString(decision.retryAfterSeconds()));
        response.getHeaders().setContentType(new MediaType("text", "plain", StandardCharsets.UTF_8));
        var body = response.bufferFactory().wrap(LIMITED_MESSAGE.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(body));
    }

    public static String clientIp(ServerHttpRequest request) {
        InetSocketAddress address = request.getRemoteAddress();
        if (address == null || address.getAddress() == null) {
            return "unknown";
        }
        return address.getAddress().getHostAddress();
    }
}
