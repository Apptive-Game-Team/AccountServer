package com.wordonline.account.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("system")
public class System {

    @Id
    private Long id;
    private Long principalId;
    private String name;
    private Long createdBy;
}
