package com.wordonline.account.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import com.wordonline.account.domain.Authority;
import com.wordonline.account.entity.System;

class TokenControllerAuthorityTest {

    @Test
    void preAuthorizeMatchesAuthorityGrantedForSuperAdmin() {
        // The scope claim carries the system-prefixed authority string, so the
        // @PreAuthorize expression must use that exact form.
        String granted = new Authority(1L, new System(1L, 1L, "ADMIN", 1L), "SUPER_ADMIN")
                .getAuthority();

        String expression = TokenController.class.getAnnotation(PreAuthorize.class).value();

        assertEquals(String.format("hasAuthority('%s')", granted), expression);
    }
}
