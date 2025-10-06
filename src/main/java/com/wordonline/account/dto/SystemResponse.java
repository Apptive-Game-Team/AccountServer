package com.wordonline.account.dto;

import com.wordonline.account.entity.System;

public record SystemResponse(
        Long id,
        String name
) {

    public SystemResponse(System system) {
        this(system.getId(), system.getName());
    }

}