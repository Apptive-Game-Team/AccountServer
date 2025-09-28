package com.wordonline.account.domain;

import java.util.List;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Member {

    private Long id;
    private Long principalId;
    private String email;
    private String passwordHash;
    private List<String> authorityStringList;

    public boolean validatePassword(String passwordPlain, PasswordEncoder passwordEncoder) {
        return passwordEncoder.matches(passwordPlain, passwordHash);
    }

    public Member(Long principalId, String email, String passwordHash) {
        this(
                null,
                principalId,
                email,
                passwordHash,
                null
        );
    }

    public Member(Member member) {
        this(member.id, member.principalId, member.email, member.passwordHash, member.authorityStringList);
    }
}
