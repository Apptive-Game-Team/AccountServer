package com.wordonline.account.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;

import com.wordonline.account.dto.ValueRequest;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class KeyValueControllerTest {

    @Test
    void noEndpointTakesCallerSuppliedMemberId() {
        for (Method method : KeyValueController.class.getDeclaredMethods()) {
            GetMapping get = method.getAnnotation(GetMapping.class);
            PutMapping put = method.getAnnotation(PutMapping.class);
            String[] paths = get != null ? get.value() : put != null ? put.value() : new String[0];
            for (String path : paths) {
                assertFalse(path.contains("/members/{"),
                        "Key-value endpoints must resolve the member from the JWT, not the path: "
                                + path);
            }
        }
        assertTrue(Arrays.stream(KeyValueController.class.getDeclaredMethods())
                .anyMatch(method -> method.getAnnotation(GetMapping.class) != null),
                "Expected the /me read endpoint to still exist");
    }

    @Test
    void setMyValue_requestBodyIsValidated() throws Exception {
        Method method = KeyValueController.class.getMethod("setMyValue", Jwt.class, String.class,
                String.class, ValueRequest.class);

        assertTrue(method.getParameters()[3].isAnnotationPresent(Validated.class),
                "PUT key-value body must be validated so an oversized value returns 400");
    }

    @Test
    void valueRequest_accepts1000CharsAndRejects1001() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertTrue(validator.validate(new ValueRequest("a".repeat(1000))).isEmpty());
            assertFalse(validator.validate(new ValueRequest("a".repeat(1001))).isEmpty());
        }
    }
}
