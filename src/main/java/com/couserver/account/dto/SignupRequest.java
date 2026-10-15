package com.couserver.account.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.nio.charset.StandardCharsets;

@Getter
public class SignupRequest {
    @NotBlank
    @Pattern(regexp = "^[a-z0-9_]{3,20}$")
    private String accountLoginId;

    @NotBlank
    @Size(min = 8, max = 64, message = "비밀번호는 8자 이상 64자 이하여야 합니다.")
    private String password;

    // BCrypt는 72바이트까지만 받는다. 한글은 한 글자에 3바이트라 24자까지 쓸 수 있다
    @JsonIgnore
    @AssertTrue(message = "비밀번호가 너무 깁니다. 한글은 24자 이하로 입력해 주세요.")
    public boolean isPasswordWithinByteLimit() {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }

    // playerNickname 제거: 닉네임은 서버가 "플레이어{accountId}"로 정한다
}