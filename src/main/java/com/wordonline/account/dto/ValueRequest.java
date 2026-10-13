package com.wordonline.account.dto;

import org.hibernate.validator.constraints.Length;

public record ValueRequest(
        // Must match the key_value.value column width (V005__widen_key_value_value.sql).
        @Length(max = 1000, message = "값은 1000자 이하이어야 합니다.") String value
) {

}
