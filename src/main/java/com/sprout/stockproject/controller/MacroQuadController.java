// src/main/java/com/sprout/stockproject/controller/MacroQuadController.java
package com.sprout.stockproject.controller;

import com.sprout.stockproject.dto.MacroQuadBannerResponse;
import com.sprout.stockproject.dto.MacroQuadInput;
import com.sprout.stockproject.dto.MacroQuadResponse;
import com.sprout.stockproject.service.MacroQuadBannerService;
import com.sprout.stockproject.service.MacroQuadLlmService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/market/quad")
public class MacroQuadController {

    private final MacroQuadLlmService llm;
    private final MacroQuadBannerService banner;

    public MacroQuadController(MacroQuadLlmService llm, MacroQuadBannerService banner) {
        this.llm = llm; this.banner = banner;
    }

    /** 4분면 스탠스(매수/중립/매도) */
    @PostMapping("/stance")
    public ResponseEntity<MacroQuadResponse> stance(@RequestBody MacroQuadInput input){
        return ResponseEntity.ok(llm.infer(input));
    }

    /** 프론트 배너 요약(성장/금리 2개 아이템) */
    @PostMapping("/banner")
    public ResponseEntity<MacroQuadBannerResponse> banner(@RequestBody MacroQuadInput input){
        var r = llm.infer(input);
        return ResponseEntity.ok(banner.build(input, r));
    }
}