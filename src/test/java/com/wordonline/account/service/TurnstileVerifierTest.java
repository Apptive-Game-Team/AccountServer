package com.wordonline.account.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import com.wordonline.account.config.TurnstileProperties;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Cloudflare is replaced by an ExchangeFunction, so no test reaches the real siteverify endpoint.
 */
class TurnstileVerifierTest {

    private static final String VERIFY_URL = "https://turnstile.test/siteverify";

    private final List<ClientRequest> requests = new ArrayList<>();

    private TurnstileVerifier verifier(String secretKey, boolean required,
            Function<ClientRequest, Mono<ClientResponse>> answer) {
        ExchangeFunction exchange = request -> {
            requests.add(request);
            return answer.apply(request);
        };
        TurnstileProperties properties = new TurnstileProperties(secretKey, required, VERIFY_URL,
                Duration.ofMillis(200));
        return new TurnstileVerifier(properties,
                WebClient.builder().exchangeFunction(exchange));
    }

    private static Mono<ClientResponse> json(HttpStatus status, String body) {
        return Mono.just(ClientResponse.create(status)
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build());
    }

    private static void assertRejected(Throwable error) {
        ResponseStatusException exception = assertInstanceOf(ResponseStatusException.class, error);
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals(TurnstileVerifier.FAILED_MESSAGE, exception.getReason());
    }

    @Test
    void successfulTokenPassesAndPostsToTheVerifyUrl() {
        TurnstileVerifier verifier = verifier("secret", false,
                request -> json(HttpStatus.OK, "{\"success\":true,\"hostname\":\"x\"}"));

        StepVerifier.create(verifier.verify("token-1", "203.0.113.7")).verifyComplete();

        ClientRequest sent = requests.get(0);
        assertEquals(VERIFY_URL, sent.url().toString());
        assertEquals("POST", sent.method().name());
    }

    @Test
    void formBodyCarriesSecretResponseAndRemoteIp() throws Exception {
        // A loopback stub stands in for Cloudflare, so the encoded form can be read back.
        java.util.concurrent.atomic.AtomicReference<String> body =
                new java.util.concurrent.atomic.AtomicReference<>();
        com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(
                new java.net.InetSocketAddress(java.net.InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/siteverify", exchange -> {
            body.set(new String(exchange.getRequestBody().readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8));
            byte[] answer = "{\"success\":true}".getBytes(java.nio.charset.StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, answer.length);
            exchange.getResponseBody().write(answer);
            exchange.close();
        });
        server.start();
        try {
            TurnstileProperties properties = new TurnstileProperties("secret", false,
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/siteverify",
                    Duration.ofSeconds(5));
            TurnstileVerifier verifier = new TurnstileVerifier(properties, WebClient.builder());

            StepVerifier.create(verifier.verify("token-1", "203.0.113.7")).verifyComplete();
        } finally {
            server.stop(0);
        }

        assertEquals("secret=secret&response=token-1&remoteip=203.0.113.7", body.get());
    }

    @Test
    void unsuccessfulTokenIsRejected() {
        TurnstileVerifier verifier = verifier("secret", false, request -> json(HttpStatus.OK,
                "{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}"));

        StepVerifier.create(verifier.verify("bad", "203.0.113.7"))
                .expectErrorSatisfies(TurnstileVerifierTest::assertRejected).verify();
    }

    @Test
    void serverErrorFromCloudflareRejectsTheSignUp() {
        TurnstileVerifier verifier = verifier("secret", false,
                request -> json(HttpStatus.INTERNAL_SERVER_ERROR, "{}"));

        StepVerifier.create(verifier.verify("token", "203.0.113.7"))
                .expectErrorSatisfies(TurnstileVerifierTest::assertRejected).verify();
    }

    @Test
    void networkErrorRejectsTheSignUp() {
        TurnstileVerifier verifier = verifier("secret", false,
                request -> Mono.error(new java.io.IOException("connection refused")));

        StepVerifier.create(verifier.verify("token", "203.0.113.7"))
                .expectErrorSatisfies(TurnstileVerifierTest::assertRejected).verify();
    }

    @Test
    void timeoutRejectsTheSignUp() {
        TurnstileVerifier verifier = verifier("secret", false, request -> Mono.never());

        StepVerifier.create(verifier.verify("token", "203.0.113.7"))
                .expectErrorSatisfies(TurnstileVerifierTest::assertRejected).verify();
    }

    @Test
    void absentTokenPassesWhenNotRequired() {
        TurnstileVerifier verifier = verifier("secret", false,
                request -> Mono.error(new AssertionError("must not call Cloudflare")));

        StepVerifier.create(verifier.verify(null, "203.0.113.7")).verifyComplete();
        StepVerifier.create(verifier.verify("  ", "203.0.113.7")).verifyComplete();
        assertTrue(requests.isEmpty());
    }

    @Test
    void absentTokenIsRejectedWhenRequired() {
        TurnstileVerifier verifier = verifier("secret", true,
                request -> Mono.error(new AssertionError("must not call Cloudflare")));

        StepVerifier.create(verifier.verify(null, "203.0.113.7"))
                .expectErrorSatisfies(TurnstileVerifierTest::assertRejected).verify();
        assertTrue(requests.isEmpty());
    }

    @Test
    void emptySecretKeySkipsVerificationEvenWhenRequired() {
        TurnstileVerifier verifier = verifier("", true,
                request -> Mono.error(new AssertionError("must not call Cloudflare")));

        StepVerifier.create(verifier.verify(null, "203.0.113.7")).verifyComplete();
        StepVerifier.create(verifier.verify("anything", "203.0.113.7")).verifyComplete();
        assertTrue(requests.isEmpty());
    }
}
