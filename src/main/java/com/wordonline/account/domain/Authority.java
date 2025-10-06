package com.wordonline.account.domain;

import com.wordonline.account.entity.AuthorityEntity;
import com.wordonline.account.entity.System;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Authority {

    private final Long id;
    private final System system;
    private String name;

    public Authority(AuthorityEntity authorityEntity, System system) {
        this(authorityEntity.getId(), system, authorityEntity.getValue());
    }

    public String getAuthority() {
        String systemName = system.getName()
                .replace("_", "")
                .replace("-", "")
                .toUpperCase();
        return String.format("%s_%s", systemName, name);
    }
}
