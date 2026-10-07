package com.couserver.master.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record MasterTableResponse(
        String tableName,
        int version,
        Object data,
        List<?> dataList
) {
    public static MasterTableResponse ofObject(String tableName, int version, Object data){
        return new MasterTableResponse(tableName, version, data, null);
    }

    public static MasterTableResponse ofArray(String tableName, int version, List<?> dataList){
        return new MasterTableResponse(tableName, version, null, dataList);
    }

}
