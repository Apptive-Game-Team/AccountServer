package com.wordonline.account.dto;

import com.wordonline.account.domain.Member;

public record MemberResponse(
        String name,
        String email
) {

    public MemberResponse(Member member) {
        this(member.getName(), member.getEmail());
    }
}
