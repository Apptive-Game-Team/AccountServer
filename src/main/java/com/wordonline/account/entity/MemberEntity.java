package com.wordonline.account.entity;

import java.util.List;
import java.util.Optional;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import com.wordonline.account.domain.Member;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Table("member")
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class MemberEntity {

    @Id
    private Long id;
    private Long principalId;
    private String name;
    private String email;
    private String passwordHash;

    public MemberEntity(Member member) {
        this(member.getId(), member.getPrincipalId(), member.getName(), member.getEmail(), member.getPasswordHash());
    }

    public Member toDomain() {
        return toDomain(null);
    }

    public Member toDomain(List<Authority> authorities) {
        return new Member(
                id,
                principalId,
                name,
                email,
                passwordHash,
                Optional.ofNullable(authorities)
                        .orElse(List.of()).stream().toList()
        );
    }
}
