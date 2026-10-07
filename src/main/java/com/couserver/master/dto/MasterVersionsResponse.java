package com.couserver.master.dto;

import com.couserver.master.entity.MasterTableVersion;

import java.time.Instant;
import java.util.List;

public record MasterVersionsResponse(List<TableVersion> tables) {
    public record TableVersion(
            String tableName,
            int version,
            Instant updatedAt
    ) {
        public static TableVersion from(MasterTableVersion v) {
            return new TableVersion(v.getTableName(), v.getVersion(), v.getUpdatedAt());
        }
    }

}
