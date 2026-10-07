package com.couserver.master.controller;

import com.couserver.master.dto.MasterTableResponse;
import com.couserver.master.dto.MasterVersionsResponse;
import com.couserver.master.service.MasterDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/master")
public class MasterController {
    private final MasterDataService masterDataService;

    @GetMapping("/versions")
    public ResponseEntity<MasterVersionsResponse> getVersions(){
        return ResponseEntity.ok(masterDataService.getVersions());
    }

    @GetMapping("/{tableName}")
    public ResponseEntity<MasterTableResponse> getTable(@PathVariable String tableName){
        return ResponseEntity.ok(masterDataService.getTable(tableName));
    }
}
