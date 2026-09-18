package com.wordonline.account.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.wordonline.account.domain.Member;

class JwtProviderTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        var keyPair = keyPairGenerator.generateKeyPair();

        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey(keyPair.getPrivate())
                .keyID("test-key")
                .build();
        jwtProvider = new JwtProvider(new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(rsaKey))));
    }

    /**
     * The client reads this claim off the decoded payload with no type coercion, so a string
     * "true" would be as wrong as a missing claim. The name is fixed: JwtHelper looks for
     * exactly `guest`.
     */
    @Test
    void theAccessTokenOfAGuestCarriesGuestAsATrueJsonBoolean() {
        JsonNode payload = payloadOf(jwtProvider.getJwt(member(true)));

        assertTrue(payload.has("guest"), payload.toString());
        assertTrue(payload.get("guest").isBoolean(), "guest must be a JSON boolean");
        assertTrue(payload.get("guest").booleanValue());
    }

    @Test
    void theAccessTokenOfARealMemberCarriesGuestAsFalse() {
        JsonNode payload = payloadOf(jwtProvider.getJwt(member(false)));

        assertTrue(payload.get("guest").isBoolean(), "guest must be a JSON boolean");
        assertFalse(payload.get("guest").booleanValue());
    }

    /**
     * The claim is added beside memberId and scope, which the game and lobby servers read.
     */
    @Test
    void theExistingClaimsSurviveBesideTheGuestClaim() {
        JsonNode payload = payloadOf(jwtProvider.getJwt(member(true)));

        assertEquals(9L, payload.get("memberId").longValue());
        assertTrue(payload.has("scope"));
    }

    private static Member member(boolean guest) {
        return new Member(9L, 1L, "tester", "tester@example.com", "hash", guest, List.of());
    }

    private static JsonNode payloadOf(String token) {
        String encodedPayload = token.split("\\.")[1];
        byte[] payload = Base64.getUrlDecoder().decode(encodedPayload);
        try {
            return OBJECT_MAPPER.readTree(new String(payload, StandardCharsets.UTF_8));
        } catch (Exception error) {
            throw new IllegalStateException("token payload is not JSON", error);
        }
    }
}
