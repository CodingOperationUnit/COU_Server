package com.couserver.social.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

// 연동 상태. 연동돼 있지 않으면 linkedAt은 응답에서 빠진다
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SocialLinkStatusResponse(boolean linked, Instant linkedAt) {

    public static SocialLinkStatusResponse notLinked() {
        return new SocialLinkStatusResponse(false, null);
    }

    public static SocialLinkStatusResponse linked(Instant linkedAt) {
        return new SocialLinkStatusResponse(true, linkedAt);
    }
}