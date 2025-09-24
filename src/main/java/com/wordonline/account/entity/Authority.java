package com.wordonline.account.entity;

import org.springframework.data.relational.core.mapping.Table;

import lombok.Getter;

@Table("Authority")
@Getter
public class Authority {

    private Long id;
    private String value;
}
