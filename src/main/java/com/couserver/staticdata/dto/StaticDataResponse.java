package com.couserver.staticdata.dto;

import tools.jackson.databind.JsonNode;

import java.util.Map;

public record StaticDataResponse(
        String version,
        Map<String, JsonNode> tables) {
}
