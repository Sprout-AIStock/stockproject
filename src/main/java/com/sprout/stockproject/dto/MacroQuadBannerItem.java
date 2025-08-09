// src/main/java/com/sprout/stockproject/dto/MacroQuadBannerItem.java
package com.sprout.stockproject.dto;

public record MacroQuadBannerItem(
        String key,        // "GROWTH" | "RATES"
        int signal,        // -1|0|1
        String tone,       // "positive"|"neutral"|"negative"
        String title,      // "성장" | "금리"
        String valueText,  // 핵심 수치 요약
        String description // 짧은 설명
) {}