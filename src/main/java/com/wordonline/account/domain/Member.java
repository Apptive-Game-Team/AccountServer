package com.wordonline.account.domain;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Member {

    private Long id;
    private Long principalId;
    private String name;
    private String email;
    private String passwordHash;
    private List<Authority> authorityList;

    public List<String> getAuthorityStringList() {
        return authorityList.stream()
                .map(Authority::getAuthority)
                .toList();
    }


    public boolean validatePassword(String passwordPlain, PasswordEncoder passwordEncoder) {
        return passwordEncoder.matches(passwordPlain, passwordHash);
    }

    public Member(Long principalId, String name, String email, String passwordHash) {
        this(
                null,
                principalId,
                name,
                email,
                passwordHash,
                null
        );
    }

    public Member(Member member) {
        this(member.id, member.principalId, member.name, member.email, member.passwordHash,
                member.getAuthorityList());
    }
}
