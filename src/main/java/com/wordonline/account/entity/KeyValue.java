package com.wordonline.account.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Table(name = "key_value")
@NoArgsConstructor
@AllArgsConstructor
public class KeyValue {

    @Id
    private Long id;
    private Long memberId;
    private Long systemId;
    private String key;

    @Setter
    private String value;

    public KeyValue(Long memberId, Long systemId, String key, String value) {
        this(null, memberId, systemId, key, value);
    }
}
