package com.wordonline.account.dto;

public record GuestAuthResponse(
        String jwt,
        String password
) {

}
