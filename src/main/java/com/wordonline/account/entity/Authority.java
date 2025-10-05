package com.wordonline.account.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table("Authority")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Authority {

    @Id
    private Long id;

    @Setter
    private String value;

    public Authority(String value) {
        this(null, value);
    }
}
