package com.wordonline.account.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.Getter;

@Table("member_authority")
@Getter
public class MemberAuthority {

    @Id
    private Long id;
    private Long memberId;
    private Long authorityId;
}
