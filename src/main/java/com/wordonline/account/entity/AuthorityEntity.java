package com.wordonline.account.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Table("authority")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AuthorityEntity {

    @Id
    private Long id;
    private Long systemId;
    @Setter
    private String value;

    public AuthorityEntity(Long systemId, String value) {
        this(null, systemId, value);
    }
}
