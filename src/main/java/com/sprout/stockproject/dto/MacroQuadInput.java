// src/main/java/com/sprout/stockproject/dto/MacroQuadInput.java
package com.sprout.stockproject.dto;

import java.time.OffsetDateTime;

public record MacroQuadInput(
        String asOf,              // ISO8601; null이면 now()로 대체
        Double gdpNow_qoq_saar_pct,
        Double payrolls_3mma_k,
        Double unemp_rate_pct,
        Double unemp_rate_change_3m_pp,
        Double claims_4wma_k,
        String claims_trend,      // "up"|"down"|"flat"|null
        Pmi pmi_mfg,
        Pmi pmi_svcs,
        Double ffr_upper_pct,
        Double core_pce_yoy_pct,
        Double core_cpi_yoy_pct,
        Integer policyRateChange3m_bps,
        String previousDecision   // "매수"|"중립"|"매도"|null
) {
    public record Pmi(Double value, Double deltaMoM) {}
    public static MacroQuadInput nowWithNulls() {
        return new MacroQuadInput(OffsetDateTime.now().toString(),
                null,null,null,null,null,null,null,null,null,null,null,null);
    }
}