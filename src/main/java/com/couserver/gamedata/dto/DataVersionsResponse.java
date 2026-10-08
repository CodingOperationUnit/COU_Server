package com.couserver.gamedata.dto;

// GET /api/v1/data/versions 응답. 다른 기획 데이터를 서버에서 받게 되면 필드를 추가한다.
public record DataVersionsResponse(int skill) {
}
