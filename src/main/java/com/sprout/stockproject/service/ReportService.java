package com.sprout.stockproject.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprout.stockproject.dto.MacroQuadInput;
import com.sprout.stockproject.dto.MacroQuadResponse;
import com.sprout.stockproject.prompt.PromptStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    private final PromptStore prompts;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Value("${prompt.macro.report.version:macro-report-daily-v1}")
    private String promptName;

    // 보고서는 기본적으로 gpt-5-mini 사용 (전역 모델과 분리)
    @Value("${openai.model.report:gpt-5-mini}")
    private String model;

    public ReportService(PromptStore prompts,
                         ObjectMapper objectMapper,
                         @Qualifier("openaiWebClient") WebClient openaiWebClient) {
        this.prompts = prompts; this.objectMapper = objectMapper; this.webClient = openaiWebClient;
    }

    public String generateDailyReport(MacroQuadInput in, MacroQuadResponse quad) {
        try {
            Map<String, Object> input = new HashMap<>();
            input.put("date", LocalDate.now().toString());
            input.put("macroInput", in);
            input.put("macro", quad);
            String payload = objectMapper.writeValueAsString(input);

            String base = prompts.get(promptName);
            String prompt = base.replace("{{INPUT_JSON}}", payload);

            Map<String, Object> req = new HashMap<>();
            req.put("model", model);
            req.put("input", prompt);
            req.put("temperature", 0);
            req.put("max_output_tokens", 1200);
            req.put("reasoning", Map.of("effort", "minimal"));
            req.put("text", Map.of("verbosity", "low"));

            String raw = webClient.post()
                    .uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(req))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractOutputText(raw);
        } catch (Exception e) {
            // 1차 폴백: gpt-5-mini로 재시도
            try {
                Map<String, Object> input = new HashMap<>();
                input.put("date", LocalDate.now().toString());
                input.put("macroInput", in);
                input.put("macro", quad);
                String payload = objectMapper.writeValueAsString(input);

                String base = prompts.get(promptName);
                String prompt = base.replace("{{INPUT_JSON}}", payload);

                Map<String, Object> req = new HashMap<>();
                req.put("model", "gpt-5-mini");
                req.put("input", prompt);
                req.put("temperature", 0);
                req.put("max_output_tokens", 1000);
                req.put("reasoning", Map.of("effort", "minimal"));
                req.put("text", Map.of("verbosity", "low"));

                String raw = webClient.post()
                        .uri("/responses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(objectMapper.writeValueAsString(req))
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                return extractOutputText(raw);
            } catch (Exception retry) {
                // 2차 폴백: 최소 마크다운 리포트(데이터만 요약)
                return buildDeterministicFallback(in, quad);
            }
        }
    }

    private String extractOutputText(String raw) throws Exception {
        var root = objectMapper.readTree(raw);
        var ot = root.get("output_text");
        if (ot != null && ot.isTextual()) return ot.asText();
        var output = root.get("output");
        if (output != null && output.isArray() && output.size() > 0) {
            var first = output.get(0);
            var content = first.get("content");
            if (content != null && content.isArray() && content.size() > 0) {
                var maybeText = content.get(0).get("text");
                if (maybeText != null && maybeText.isTextual()) return maybeText.asText();
            }
        }
        return raw;
    }

    private String buildDeterministicFallback(MacroQuadInput in, MacroQuadResponse quad) {
        StringBuilder md = new StringBuilder();
        md.append("# Daily Macro Report (Fallback)\n\n");
        md.append("생성 시각: ").append(LocalDate.now()).append("\n");
        md.append("기준 시점(asOf): ").append(quad.asOf()).append("\n\n");
        md.append("## 시장 스탠스 요약\n");
        md.append("- 결정: ").append(quad.decision()).append("\n");
        md.append("- 성장 신호: ").append(quad.growthSignal()).append(" / 금리 신호: ").append(quad.rateSignal()).append("\n");
        md.append("- 점수: ").append(quad.score()).append(" (확신도 ").append(String.format("%.0f%%", quad.confidence()*100)).append(")\n\n");
        if (quad.facts()!=null && !quad.facts().isEmpty()) {
            md.append("### 근거 팩트\n");
            for (String f : quad.facts()) md.append("- ").append(f).append("\n");
            md.append("\n");
        }
        md.append("## 입력 데이터(요약)\n");
        md.append("- GDPNow(q/q saar): ").append(in.gdpNow_qoq_saar_pct()).append("\n");
        md.append("- Payrolls 3mma(k): ").append(in.payrolls_3mma_k()).append("\n");
        md.append("- Unemp rate(%): ").append(in.unemp_rate_pct()).append(" (Δ3m ").append(in.unemp_rate_change_3m_pp()).append(")\n");
        md.append("- Claims 4wma(k): ").append(in.claims_4wma_k()).append(" (trend ").append(in.claims_trend()).append(")\n");
        md.append("- PMI mfg/svcs: ").append(in.pmi_mfg()).append(" / ").append(in.pmi_svcs()).append("\n");
        md.append("- FFR upper(%): ").append(in.ffr_upper_pct()).append("\n");
        md.append("- Core PCE/CPI YoY(%): ").append(in.core_pce_yoy_pct()).append(" / ").append(in.core_cpi_yoy_pct()).append("\n\n");
        md.append("(본 리포트는 LLM 오류로 인해 축약 버전으로 생성되었습니다.)\n");
        return md.toString();
    }
}


