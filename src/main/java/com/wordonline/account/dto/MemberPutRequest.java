package com.wordonline.account.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import org.hibernate.validator.constraints.Length;

public record MemberPutRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "이메일이 올바르지 않습니다.") String email,
        @Length(min = 0, max = 31, message = "이름은 31자 이하이어야 합니다.") String name,
        String lastPassword,
        @NotBlank(message = "비밀번호는 필수입니다.")
        @Length(min = 8, message = "비밀번호는 8자 이상이어야 합니다.") String password
) {

}