package com.couserver.account.dto;

import jakarta.validation.constraints.NotBlank;

public record ChangeNicknameRequest(
        @NotBlank(message = "변경할 닉네임을 입력해 주세요.")
        String nickname
) {
}
