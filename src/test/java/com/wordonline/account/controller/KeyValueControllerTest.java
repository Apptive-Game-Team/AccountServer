package com.wordonline.account.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;

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
}
