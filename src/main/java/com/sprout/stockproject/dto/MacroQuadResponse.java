// src/main/java/com/sprout/stockproject/dto/MacroQuadResponse.java
package com.sprout.stockproject.dto;

import java.util.List;

public record MacroQuadResponse(
        int score,                  // -3..+3
        int growthSignal,           // -1|0|1
        int rateSignal,             // -1|0|1
        String decision,            // "매수"|"중립"|"매도"
        String quadrantLabel,       // 위 5가지 중 하나
        double confidence,          // 0..1
        int usedSignals,            // 0..2
        List<String> facts,
        String asOf
) {}