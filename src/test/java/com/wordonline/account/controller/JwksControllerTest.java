package com.wordonline.account.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

import reactor.test.StepVerifier;

class JwksControllerTest {

    private JwksController jwksController;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        var keyPair = keyPairGenerator.generateKeyPair();

        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .keyUse(KeyUse.SIGNATURE)
                .algorithm(JWSAlgorithm.RS256)
                .keyID("2026-04-06-01")
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);

        jwksController = new JwksController(jwkSet);
    }

    @Test
    void getJwks_returnsPublicKeySet() {
        StepVerifier.create(jwksController.getJwks())
                .assertNext(jwks -> {
                    assertNotNull(jwks);
                    assertTrue(jwks.containsKey("keys"));

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> keys = (List<Map<String, Object>>) jwks.get("keys");
                    assertFalse(keys.isEmpty());

                    Map<String, Object> key = keys.get(0);
                    assertEquals("RSA", key.get("kty"));
                    assertNotNull(key.get("n"));
                    assertNotNull(key.get("e"));
                    assertEquals("sig", key.get("use"));
                    assertEquals("RS256", key.get("alg"));
                    assertEquals("2026-04-06-01", key.get("kid"));
                    // Private key components must NOT be present
                    assertFalse(key.containsKey("d"));
                    assertFalse(key.containsKey("p"));
                    assertFalse(key.containsKey("q"));
                })
                .verifyComplete();
    }
}
