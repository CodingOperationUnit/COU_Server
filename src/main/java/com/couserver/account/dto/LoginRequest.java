package com.couserver.account.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class LoginRequest {
    @NotBlank
    private String accountLoginId;

    @NotBlank
    private String password;
}
