package com.wordonline.account.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.wordonline.account.config.TurnstileProperties;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

/**
 * Checks a Cloudflare Turnstile token before a member is registered.
 *
 * <p>The token is optional because the Android, desktop and WebGL game clients cannot render the
 * Turnstile widget and send none. Set turnstile.required once every registered sign-up goes
 * through the website. A token that is present is always checked, and a failed check, a network
 * error or a timeout all reject the request.
 */
@Slf4j
@Service
public class TurnstileVerifier {

    static final String FAILED_MESSAGE = "자동 가입 방지 확인에 실패했습니다. 다시 시도해 주세요.";
    // Cloudflare documents 2048 characters as the longest token.
    private static final int MAX_TOKEN_LENGTH = 2048;

    private final TurnstileProperties properties;
    private final WebClient webClient;

    public TurnstileVerifier(TurnstileProperties properties, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.webClient = webClientBuilder.build();
        if (!properties.enabled()) {
            log.warn("[Turnstile] TURNSTILE_SECRET_KEY is empty, so sign-up captcha verification "
                    + "is disabled and every captchaToken is ignored.");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SiteVerifyResponse(boolean success) {

    }

    public Mono<Void> verify(String token, String remoteIp) {
        if (!properties.enabled()) {
            return Mono.empty();
        }
        boolean absent = token == null || token.isBlank();
        if (absent) {
            return properties.required() ? Mono.error(rejection()) : Mono.empty();
        }
        if (token.length() > MAX_TOKEN_LENGTH) {
            return Mono.error(rejection());
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", properties.secretKey());
        form.add("response", token);
        if (remoteIp != null && !remoteIp.isBlank() && !"unknown".equals(remoteIp)) {
            form.add("remoteip", remoteIp);
        }

        return webClient.post()
                .uri(properties.verifyUrl())
                .body(BodyInserters.fromFormData(form))
                .retrieve()
                .bodyToMono(SiteVerifyResponse.class)
                .timeout(properties.timeout())
                .onErrorResume(error -> {
                    log.warn("[Turnstile] verification request failed: {}", error.toString());
                    return Mono.error(rejection());
                })
                .map(SiteVerifyResponse::success)
                .defaultIfEmpty(false)
                .flatMap(success -> success ? Mono.<Void>empty() : Mono.error(rejection()))
                .then();
    }

    private static ResponseStatusException rejection() {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, FAILED_MESSAGE);
    }
}
