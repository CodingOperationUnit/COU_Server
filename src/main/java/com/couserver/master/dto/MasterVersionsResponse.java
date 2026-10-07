package com.couserver.master.dto;

import com.couserver.master.entity.MasterTableVersion;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.List;

public record MasterVersionsResponse(List<TableVersion> tables) {
    public record TableVersion(
            String tableName,
            int version,
            @JsonFormat(pattern="yyyy-MM-dd'T'HH:mm:ss")
            LocalDateTime updatedAt
    ) {
        public static TableVersion from(MasterTableVersion v) {
            return new TableVersion(v.getTableName(), v.getVersion(), v.getUpdatedAt());
        }
    }

}
