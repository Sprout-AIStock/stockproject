package com.sprout.stockproject.controller;

import com.sprout.stockproject.api.ApiResponse;
import com.sprout.stockproject.api.UnprocessableException;
import com.sprout.stockproject.cache.MacroSnapshotCache;
import com.sprout.stockproject.dto.MacroQuadInput;
import com.sprout.stockproject.service.StockInfoService;
import com.sprout.stockproject.service.StockOpinionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/stock")
public class StockOpinionController {

    private final MacroSnapshotCache cache;
    private final StockInfoService stockInfoService;
    private final StockOpinionService stockOpinionService;

    public StockOpinionController(MacroSnapshotCache cache, StockInfoService stockInfoService, StockOpinionService stockOpinionService) {
        this.cache = cache;
        this.stockInfoService = stockInfoService;
        this.stockOpinionService = stockOpinionService;
    }

    @PostMapping("/opinion")
    public ResponseEntity<ApiResponse<Map<String, Object>>> opinion(
            @RequestParam String code,
            @RequestParam(required = false, defaultValue = "mid") String horizon,
            @RequestParam(required = false, defaultValue = "normal") String risk
    ) {
        Optional<MacroQuadInput> opt = cache.latest();
        if (opt.isEmpty()) throw new UnprocessableException("거시 스냅샷이 아직 준비되지 않았습니다(잠시 후 재시도)");
        var stock = stockInfoService.fetch(code);
        var opinion = stockOpinionService.buildOpinion(opt.get(), stock, horizon, risk);
        return ResponseEntity.ok(ApiResponse.ok(opinion));
    }
}


