package com.couserver.account.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AuthAccount {
    private final int accountId;
    private final Long playerId;
}
