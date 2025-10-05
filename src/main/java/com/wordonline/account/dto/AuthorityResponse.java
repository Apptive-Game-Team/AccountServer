package com.wordonline.account.dto;

import com.wordonline.account.entity.Authority;

public record AuthorityResponse(
        Long id,
        String name
) {

    public AuthorityResponse(Authority authority) {
        this(authority.getId(), authority.getValue());
    }
}