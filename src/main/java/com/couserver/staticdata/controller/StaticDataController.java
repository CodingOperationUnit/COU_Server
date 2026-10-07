package com.couserver.staticdata.controller;

import com.couserver.staticdata.dto.StaticDataResponse;
import com.couserver.staticdata.service.StaticDataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/static-data")
public class StaticDataController {
    private final StaticDataService staticDataService;

    // 클라이언트가 가진 버전과 같으면 본문 없이 204로 응답한다
    @GetMapping
    public ResponseEntity<StaticDataResponse> getStaticData(@RequestParam(required = false) String version) {
        if (staticDataService.getVersion().equals(version)) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(new StaticDataResponse(staticDataService.getVersion(), staticDataService.getTables()));
    }
}
