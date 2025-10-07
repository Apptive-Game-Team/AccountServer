package com.wordonline.account.dto;

import com.wordonline.account.domain.Member;
import com.wordonline.account.entity.MemberEntity;
import com.wordonline.account.repository.MemberRepository;

public record MemberResponse(
        Long id,
        String name,
        String email
) {
    public MemberResponse(Member member) {
        this(member.getId(), member.getName(), member.getEmail());
    }
    public static MemberResponse from(MemberEntity memberEntity) {
        return new MemberResponse(
                memberEntity.getId(),
                memberEntity.getName(),
                memberEntity.getEmail()
        );
    }
}