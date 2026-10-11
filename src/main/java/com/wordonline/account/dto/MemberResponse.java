package com.wordonline.account.dto;

import com.wordonline.account.domain.Member;
import com.wordonline.account.entity.MemberEntity;

public record MemberResponse(
        Long id,
        String name,
        String email,
        boolean guest
) {

    public MemberResponse(Member member) {
        this(member.getId(), member.getName(), member.getEmail(), member.isGuest());
    }

    public static MemberResponse from(MemberEntity memberEntity) {
        return new MemberResponse(
                memberEntity.getId(),
                memberEntity.getName(),
                memberEntity.getEmail(),
                memberEntity.isGuest()
        );
    }
}