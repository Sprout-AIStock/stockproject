// src/main/java/com/sprout/stockproject/service/MacroQuadLlmService.java
package com.sprout.stockproject.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprout.stockproject.dto.MacroQuadInput;
import com.sprout.stockproject.dto.MacroQuadResponse;
import com.sprout.stockproject.prompt.PromptStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class MacroQuadLlmService {

    private final PromptStore prompts;
    private final ObjectMapper om;
    private final WebClient webClient;

    @Value("${prompt.macro.quad.version:macro-quad-v1}")
    private String promptName;
    @Value("${openai.model:gpt-4.1-mini}")
    private String model;
    @Value("${openai.api.base:https://api.openai.com/v1}")
    private String openaiBase;
    @Value("${openai.api.key:}")
    private String openaiKey;

    public MacroQuadLlmService(PromptStore prompts, ObjectMapper om, @Qualifier("openaiWebClient") WebClient openaiWebClient) {
        this.prompts = prompts; this.om = om;
        this.webClient = openaiWebClient;
    }

    public MacroQuadResponse infer(MacroQuadInput input) {
        try {
            var effective = ensureAsOf(input);
            String inputJson = om.writeValueAsString(effective);

            String base = prompts.get(promptName);
            String prompt = base.replace("{{INPUT_JSON}}", inputJson);

            Map<String,Object> schema = responseSchema();
            Map<String,Object> req = new HashMap<>();
            req.put("model", model);
            req.put("input", prompt);
            req.put("temperature", 0);
            req.put("max_output_tokens", 400);
            Map<String,Object> respFmt = new HashMap<>();
            respFmt.put("type", "json_schema");
            respFmt.put("json_schema", Map.of("name","macro_quad","schema", schema));
            req.put("response_format", respFmt);

            String raw = webClient.post()
                    .uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + (openaiKey!=null && !openaiKey.isBlank()? openaiKey : System.getenv("OPENAI_API_KEY")))
                    .bodyValue(om.writeValueAsString(req))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            String json = extractOutputJson(raw);
            var parsed = om.readValue(json, MacroQuadResponse.class);
            validate(parsed);
            return parsed;
        } catch (Exception e) {
            throw new RuntimeException("Macro quad inference failed", e);
        }
    }

    /** Safe inference: returns neutral fallback on any error. */
    public MacroQuadResponse inferSafe(MacroQuadInput input) {
        try {
            return infer(input);
        } catch (Exception e) {
            MacroQuadInput effective = ensureAsOf(input);
            return new MacroQuadResponse(0, 0, 0, "중립", "Mixed", 0.1, 0,
                    java.util.List.of("LLM 오류 폴백"), effective.asOf());
        }
    }

    private MacroQuadInput ensureAsOf(MacroQuadInput in){
        if (in == null) return MacroQuadInput.nowWithNulls();
        if (in.asOf()==null || in.asOf().isBlank()){
            return new MacroQuadInput(OffsetDateTime.now().toString(),
                    in.gdpNow_qoq_saar_pct(), in.payrolls_3mma_k(), in.unemp_rate_pct(),
                    in.unemp_rate_change_3m_pp(), in.claims_4wma_k(), in.claims_trend(),
                    in.pmi_mfg(), in.pmi_svcs(), in.ffr_upper_pct(),
                    in.core_pce_yoy_pct(), in.core_cpi_yoy_pct(),
                    in.policyRateChange3m_bps(), in.previousDecision());
        }
        return in;
    }

    private void validate(MacroQuadResponse r){
        if (r.decision()==null) throw new IllegalArgumentException("decision null");
        if (r.score() < -3 || r.score() > 3) throw new IllegalArgumentException("score range -3..3");
        if (r.growthSignal() < -1 || r.growthSignal() > 1) throw new IllegalArgumentException("growth signal range");
        if (r.rateSignal() < -1 || r.rateSignal() > 1) throw new IllegalArgumentException("rate signal range");
    }

    private String extractOutputJson(String raw) throws Exception {
        JsonNode root = om.readTree(raw);
        JsonNode ot = root.get("output_text");
        if (ot != null && ot.isTextual()) return ot.asText();
        JsonNode output = root.get("output");
        if (output != null && output.isArray() && output.size() > 0) {
            JsonNode first = output.get(0);
            JsonNode content = first.get("content");
            if (content != null && content.isArray() && content.size() > 0) {
                JsonNode maybeText = content.get(0).get("text");
                if (maybeText != null && maybeText.isTextual()) return maybeText.asText();
            }
        }
        return raw; // 모델이 바로 JSON을 내는 경우
    }

    private Map<String,Object> responseSchema() {
        return Map.of(
                "$schema","http://json-schema.org/draft-07/schema#",
                "type","object",
                "additionalProperties", false,
                "properties", Map.of(
                        "score", Map.of("type","integer","minimum",-3,"maximum",3),
                        "growthSignal", Map.of("type","integer","enum", new int[]{-1,0,1}),
                        "rateSignal", Map.of("type","integer","enum", new int[]{-1,0,1}),
                        "decision", Map.of("type","string","enum", new String[]{"매수","중립","매도"}),
                        "quadrantLabel", Map.of("type","string","enum", new String[]{
                                "HighGrowth+FriendlyRates","HighGrowth+RestrictiveRates",
                                "LowGrowth+FriendlyRates","LowGrowth+RestrictiveRates","Mixed"}),
                        "confidence", Map.of("type","number","minimum",0,"maximum",1),
                        "usedSignals", Map.of("type","integer","minimum",0,"maximum",2),
                        "facts", Map.of("type","array","items", Map.of("type","string")),
                        "asOf", Map.of("type","string")
                ),
                "required", new String[]{"score","growthSignal","rateSignal","decision","quadrantLabel","confidence","usedSignals","facts","asOf"}
        );
    }
}