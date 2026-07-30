package com.wordonline.account.controller;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Parameter;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;

import com.wordonline.account.dto.MemberPutRequest;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class MemberControllerValidationTest {

    @Test
    void putMember_requestBodyIsValidated() throws Exception {
        Parameter body = MemberController.class
                .getMethod("putMember", Jwt.class, MemberPutRequest.class)
                .getParameters()[1];

        assertTrue(body.isAnnotationPresent(Validated.class),
                "PUT /api/members/me body must be validated");
    }

    @Test
    void memberPutRequest_rejectsInvalidEmailAndWeakPassword() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            assertFalse(validator.validate(
                    new MemberPutRequest("not-an-email", "name", "old", "12345678")).isEmpty());
            assertFalse(validator.validate(
                    new MemberPutRequest("a@b.com", "name", "old", "1")).isEmpty());
            assertFalse(validator.validate(
                    new MemberPutRequest("a@b.com", "name", "old", null)).isEmpty());
            assertFalse(validator.validate(
                    new MemberPutRequest(null, "name", "old", "12345678")).isEmpty());
            assertTrue(validator.validate(
                    new MemberPutRequest("a@b.com", "name", "old", "12345678")).isEmpty());
        }
    }
}
