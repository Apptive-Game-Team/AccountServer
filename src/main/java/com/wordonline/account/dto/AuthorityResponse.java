package com.wordonline.account.dto;

import com.wordonline.account.domain.Authority;

public record AuthorityResponse(
        Long id,
        SystemResponse system,
        String name,
        String authority
) {

    public AuthorityResponse(Authority authority) {
        this(
                authority.getId(),
                new SystemResponse(authority.getSystem()),
                authority.getName(),
                authority.getAuthority()
        );
    }

}