package com.wordonline.account.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table("member_authority")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberAuthority {

    @Id
    private Long id;
    private Long memberId;
    private Long authorityId;

    public MemberAuthority(Long memberId, Long authorityId) {
        this(null, memberId, authorityId);
    }
}
