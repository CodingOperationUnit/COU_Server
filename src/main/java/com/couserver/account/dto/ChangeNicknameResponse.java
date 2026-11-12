package com.couserver.account.dto;

public record ChangeNicknameResponse(
        String playerNickname,
        int nicknameChangeTicketCount
) {
}
