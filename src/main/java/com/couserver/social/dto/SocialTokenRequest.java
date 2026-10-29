package com.couserver.social.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class SocialTokenRequest {
    @NotBlank
    @Size(max = 8192, message = "소셜 로그인 정보가 올바르지 않습니다.")
    private String idToken;
}