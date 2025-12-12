package com.wordonline.account.dto;

import java.util.List;

public record ServerTokenRequest(
        Long expiryMinutes, // null means infinite
        List<Long> authorityIds
) {
}
