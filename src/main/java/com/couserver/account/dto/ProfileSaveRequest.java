package com.couserver.account.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ProfileSaveRequest {
    @NotBlank
    @Size(max = 20)
    private String playerNickname;

    @Min(1)
    private int accountLevel;

    @Min(0)
    private int accountExp;
}