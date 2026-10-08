package com.couserver.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class SignupRequest {
    @NotBlank
    @Pattern(regexp = "^[a-z0-9_]{3,20}$")
    private String accountLoginId;

    @NotBlank
    private String password;

    // playerNickname 제거: 닉네임은 서버가 "플레이어{accountId}"로 정한다
    // @NotBlank @Size(max = 20)
    // private String playerNickname;
}
