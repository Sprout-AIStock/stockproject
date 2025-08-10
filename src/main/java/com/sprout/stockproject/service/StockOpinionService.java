package com.sprout.stockproject.service;

import com.sprout.stockproject.dto.MacroQuadInput;
import com.sprout.stockproject.dto.MacroQuadResponse;
import com.sprout.stockproject.dto.StockDetailDto;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class StockOpinionService {

    private final MacroQuadLlmService macro;
    private final StockOpinionLlmService opinionLlm;

    public StockOpinionService(MacroQuadLlmService macro, StockOpinionLlmService opinionLlm) {
        this.macro = macro;
        this.opinionLlm = opinionLlm;
    }

    public Map<String, Object> buildOpinion(MacroQuadInput macroInput, StockDetailDto stock, String horizon, String risk) {
        MacroQuadResponse quad = macro.inferSafe(macroInput);
        StockOpinionLlmService.OpinionResult res = opinionLlm.infer(quad, stock, horizon, risk);

        Map<String, Object> out = new HashMap<>();
        out.put("stance", res.stance());
        out.put("confidence", res.confidence());
        out.put("reasons", res.reasons());
        out.put("asOf", res.asOf());
        out.put("macroFacts", quad.facts());
        out.put("stock", Map.of(
                "code", stock.getCode(),
                "name", stock.getName(),
                "price", stock.getPrice(),
                "marketCap", stock.getMarketCap(),
                "per", stock.getPer(),
                "pbr", stock.getPbr()
        ));
        out.put("horizon", horizon);
        out.put("risk", risk);
        return out;
    }
}


