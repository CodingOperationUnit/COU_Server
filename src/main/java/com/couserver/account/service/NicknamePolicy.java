
package com.couserver.account.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class NicknamePolicy {

    // 완성형 한글, 영문, 숫자만 허용 (2~12자)
    private static final Pattern ALLOWED_PATTERN =
            Pattern.compile("^[가-힣A-Za-z0-9]{2,12}$");

    // 서버가 자동 생성하는 기본 닉네임은 예약
    private static final Pattern DEFAULT_NICKNAME_PATTERN =
            Pattern.compile("^플레이어[0-9]+$");

    // 운영진 사칭 방지를 위한 예약어
    private static final List<String> RESERVED_PREFIXES = List.of(
            "관리자",
            "운영자",
            "admin",
            "gm"
    );

    public void validate(String nickname) {
        if (nickname == null ||
                !ALLOWED_PATTERN.matcher(nickname).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "닉네임은 2~12자의 한글, 영문, 숫자만 사용할 수 있습니다."
            );
        }

        if (DEFAULT_NICKNAME_PATTERN.matcher(nickname).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "자동 생성 닉네임 형식은 사용할 수 없습니다."
            );
        }

        String lowerNickname = nickname.toLowerCase(Locale.ROOT);

        for (String prefix : RESERVED_PREFIXES) {
            if (lowerNickname.startsWith(prefix)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "사용할 수 없는 예약어가 포함된 닉네임입니다."
                );
            }
        }
    }
}
