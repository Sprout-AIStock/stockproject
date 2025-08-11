// src/main/java/com/sprout/stockproject/service/MacroQuadBannerService.java
package com.sprout.stockproject.service;

import com.sprout.stockproject.dto.MacroQuadBannerItem;
import com.sprout.stockproject.dto.MacroQuadBannerResponse;
import com.sprout.stockproject.dto.MacroQuadInput;
import com.sprout.stockproject.dto.MacroQuadResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MacroQuadBannerService {

    public MacroQuadBannerResponse build(MacroQuadInput input, MacroQuadResponse quad) {
        var growthItem = new MacroQuadBannerItem(
                "GROWTH",
                quad.growthSignal(),
                tone(quad.growthSignal()),
                "성장",
                growthValueText(input),
                growthDesc(quad.growthSignal())
        );
        var rateItem = new MacroQuadBannerItem(
                "RATES",
                quad.rateSignal(),
                tone(quad.rateSignal()),
                "금리",
                rateValueText(input),
                rateDesc(quad.rateSignal())
        );
        String decisionKo = quad.decision();
        String stance = switch (decisionKo) {
            case "매수" -> "buy";
            case "매도" -> "sell";
            default -> "neutral";
        };
        String icon = switch (decisionKo) {
            case "매수" -> "bull";
            case "매도" -> "bear";
            default -> "neutral";
        };
        String headline = switch (quad.quadrantLabel()) {
            case "HighGrowth+FriendlyRates" -> "성장 우세·금리 우호";
            case "HighGrowth+RestrictiveRates" -> "성장 우세·금리 제약";
            case "LowGrowth+FriendlyRates" -> "성장 둔화·금리 우호";
            case "LowGrowth+RestrictiveRates" -> "성장 둔화·금리 제약";
            default -> "혼조";
        };
        String subtext = growthValueText(input) + " | " + rateValueText(input);
        String color = tone(quad.score());

        return new MacroQuadBannerResponse(
                quad.decision(), quad.quadrantLabel(), quad.score(), quad.confidence(), quad.asOf(),
                List.of(growthItem, rateItem), stance, headline, subtext, color, icon
        );
    }

    private String tone(int s) { return s>0?"positive": s<0?"negative":"neutral"; }

    private String growthDesc(int s){
        return s>0 ? "성장 지표 우세" : s<0 ? "성장 둔화 신호" : "혼조";
    }
    private String rateDesc(int s){
        return s>0 ? "금리 여건 우호" : s<0 ? "긴축/높은 실질금리" : "중립";
    }

    private String growthValueText(MacroQuadInput in){
        String gdp = in.gdpNow_qoq_saar_pct()==null? "GDPNow n/a" : String.format("GDPNow %.1f%%", in.gdpNow_qoq_saar_pct());
        String nfp = in.payrolls_3mma_k()==null? "NFP 3M n/a" : String.format("NFP3M +%.0fk", in.payrolls_3mma_k());
        String ur  = in.unemp_rate_pct()==null? "U n/a" : String.format("U %.1f%%", in.unemp_rate_pct());
        String pmiM= in.pmi_mfg()==null || in.pmi_mfg().value()==null ? "PMI M n/a" : String.format("PMI M %.1f", in.pmi_mfg().value());
        String pmiS= in.pmi_svcs()==null || in.pmi_svcs().value()==null ? "PMI S n/a" : String.format("PMI S %.1f", in.pmi_svcs().value());
        return String.join(" · ", gdp, nfp, ur, pmiM, pmiS);
    }

    private String rateValueText(MacroQuadInput in){
        String ffr = in.ffr_upper_pct()==null? "FFR n/a" : String.format("FFR %.2f%%", in.ffr_upper_pct());
        Double infl = in.core_pce_yoy_pct()!=null? in.core_pce_yoy_pct() : in.core_cpi_yoy_pct();
        String inf = infl==null? "Core n/a" : String.format("Core %.1f%%", infl);
        String d3m = in.policyRateChange3m_bps()==null? "3m Δ n/a" : String.format("3m Δ %s%dbp", in.policyRateChange3m_bps()>=0?"+":"", in.policyRateChange3m_bps());
        String real = (in.ffr_upper_pct()!=null && infl!=null) ? String.format("real %.1f%%", in.ffr_upper_pct()-infl) : "real n/a";
        return String.join(" · ", ffr, inf, real, d3m);
    }
}