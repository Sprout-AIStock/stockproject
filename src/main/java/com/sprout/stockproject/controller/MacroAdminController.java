package com.sprout.stockproject.controller;

import com.sprout.stockproject.api.ApiResponse;
import com.sprout.stockproject.cache.MacroSnapshotCache;
import com.sprout.stockproject.dto.MacroQuadInput;
import com.sprout.stockproject.service.MacroQuadIngestService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/market/quad")
public class MacroAdminController {

    private final MacroQuadIngestService ingestService;
    private final MacroSnapshotCache cache;

    public MacroAdminController(MacroQuadIngestService ingestService, MacroSnapshotCache cache) {
        this.ingestService = ingestService;
        this.cache = cache;
    }

    /** 수동으로 즉시 거시 스냅샷을 적재하고 캐시에 반영 */
    @PostMapping("/ingest")
    public ResponseEntity<ApiResponse<Map<String, Object>>> ingestNow() {
        MacroQuadInput input = ingestService.buildLatestInput();
        cache.put(input);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "asOf", input.asOf()
        )));
    }
}


