package com.couserver.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class ProfileSaveRequest {
    @NotBlank
    @Size(max = 20)
    private String playerNickname;

    // 레벨과 경험치는 전투 결과로만 바뀐다. 생략하면 null
    @Deprecated
    private Integer accountLevel;

    @Deprecated
    private Integer accountExp;
}