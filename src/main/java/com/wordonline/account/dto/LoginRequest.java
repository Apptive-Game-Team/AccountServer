package com.wordonline.account.dto;

import jakarta.validation.constraints.Email;

import org.hibernate.validator.constraints.Length;

public record LoginRequest(
        @Email String email,
        @Length(min = 8) String password
) {

    public LoginRequest(JoinRequest joinRequest) {
        this(joinRequest.email(), joinRequest.password());
    }
}
