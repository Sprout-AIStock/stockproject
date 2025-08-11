// src/main/java/com/sprout/stockproject/dto/MacroQuadBannerResponse.java
package com.sprout.stockproject.dto;

import java.util.List;

public record MacroQuadBannerResponse(
        String decision,        // 매수/중립/매도
        String quadrantLabel,   // 사분면 라벨
        int score,              // -3..+3
        double confidence,      // 0..1
        String asOf,
        List<MacroQuadBannerItem> items,
        String stance,          // 프론트 요구: 동일 값 복제 제공
        String headline,        // 프론트 요구: 짧은 헤드라인
        String subtext,         // 프론트 요구: 보조 설명
        String color,           // "positive"|"neutral"|"negative"
        String icon             // "bull"|"neutral"|"bear" 등 단순 토큰
) {}