// src/main/java/com/sprout/stockproject/dto/MacroQuadBannerResponse.java
package com.sprout.stockproject.dto;

import java.util.List;

public record MacroQuadBannerResponse(
        String decision,        // 매수/중립/매도
        String quadrantLabel,   // 사분면 라벨
        int score,              // -3..+3
        double confidence,      // 0..1
        String asOf,
        List<MacroQuadBannerItem> items
) {}