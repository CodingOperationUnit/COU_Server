package com.couserver.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class LoginRequest {
    // 가입 규칙상 20자를 넘는 ID는 없다. 긴 문자열로 시도 제한 키를 늘리지 못하게 한다
    @NotBlank
    @Size(max = 20, message = "아이디 또는 비밀번호가 올바르지 않습니다.")
    private String accountLoginId;

    // 기존 계정에는 비밀번호 정책을 적용하지 않는다. 지나치게 긴 입력만 막는다
    @NotBlank
    @Size(max = 128, message = "아이디 또는 비밀번호가 올바르지 않습니다.")
    private String password;
}