package com.sprout.stockproject.controller;

import com.sprout.stockproject.api.ApiResponse;
import com.sprout.stockproject.cache.MacroSnapshotCache;
import com.sprout.stockproject.dto.MacroQuadBannerResponse;
import com.sprout.stockproject.dto.MacroQuadInput;
import com.sprout.stockproject.dto.MacroQuadResponse;
import com.sprout.stockproject.service.MacroQuadBannerService;
import com.sprout.stockproject.service.MacroQuadLlmService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/market/quad")
public class MacroQuadLatestController {

    private final MacroSnapshotCache cache;
    private final MacroQuadLlmService llm;
    private final MacroQuadBannerService banner;

    public MacroQuadLatestController(MacroSnapshotCache cache, MacroQuadLlmService llm, MacroQuadBannerService banner) {
        this.cache = cache; this.llm = llm; this.banner = banner;
    }

    @GetMapping("/input/latest")
    public ResponseEntity<?> latestInput() {
        Optional<MacroQuadInput> opt = cache.latest();
        if (opt.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(ApiResponse.ok(opt.get()));
    }

    @GetMapping("/stance/latest")
    public ResponseEntity<?> latestStance() {
        Optional<MacroQuadInput> opt = cache.latest();
        if (opt.isEmpty()) return ResponseEntity.noContent().build();
        MacroQuadResponse r = llm.inferSafe(opt.get());
        return ResponseEntity.ok(ApiResponse.ok(r));
    }

    @GetMapping("/banner/latest")
    public ResponseEntity<?> latestBanner() {
        Optional<MacroQuadInput> opt = cache.latest();
        if (opt.isEmpty()) return ResponseEntity.noContent().build();
        MacroQuadInput in = opt.get();
        MacroQuadResponse r = llm.inferSafe(in);
        MacroQuadBannerResponse b = banner.build(in, r);
        return ResponseEntity.ok(ApiResponse.ok(b));
    }
}


