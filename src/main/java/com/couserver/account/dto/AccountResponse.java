package com.couserver.account.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.time.LocalDateTime;

@Getter
@RequiredArgsConstructor
public class AccountResponse {
    private final Integer accountId;
    private final String accountLoginId;
    private final LocalDateTime accountCreatedAt;
    private final LocalDateTime accountLastLoginAt;
}
