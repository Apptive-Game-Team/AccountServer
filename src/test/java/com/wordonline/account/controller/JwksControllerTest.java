package com.wordonline.account.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;

import reactor.test.StepVerifier;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;

@ExtendWith(MockitoExtension.class)
class JwksControllerTest {

    private JwksController jwksController;

    @BeforeEach
    void setUp() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        var keyPair = keyPairGenerator.generateKeyPair();

        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .keyUse(KeyUse.SIGNATURE)
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
                    // Private key components must NOT be present
                    assertFalse(key.containsKey("d"));
                    assertFalse(key.containsKey("p"));
                    assertFalse(key.containsKey("q"));
                })
                .verifyComplete();
    }
}
