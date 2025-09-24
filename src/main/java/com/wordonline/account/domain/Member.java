package com.wordonline.account.domain;

import java.util.List;

import org.springframework.security.crypto.bcrypt.BCrypt;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Member {

    private Long id;
    private Long principalId;
    private String email;
    private String passwordHash;
    private List<String> authorities;

    public boolean validatePassword(String passwordPlain) {
        return BCrypt.checkpw(passwordPlain, passwordHash);
    }

    public static Member createWithPasswordPlain(Long principalId, String email, String password) {
        return new Member(
                null,
                principalId,
                email,
                BCrypt.hashpw(password, BCrypt.gensalt()),
                null
        );
    }
}
