package com.wordonline.account.dto;

import jakarta.validation.constraints.Email;

import org.hibernate.validator.constraints.Length;

public record JoinRequest(
        @Email String email,
        @Length(min = 0, max = 31) String name,
        @Length(min = 8) String password
) {

}