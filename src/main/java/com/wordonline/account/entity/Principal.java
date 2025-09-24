package com.wordonline.account.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Getter;

@Table("principal")
@Getter
public class Principal {

    @Id
    private Long id;
}
