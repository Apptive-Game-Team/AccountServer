package com.wordonline.account.entity;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Table("system")
@NoArgsConstructor
@AllArgsConstructor
public class System {

    @Id
    private Long id;
    private Long principalId;
    @Setter
    private String name;
    private Long createdBy;

    public System(Long principalId, String name, Long createdBy) {
        this(null, principalId, name, createdBy);
    }
}
