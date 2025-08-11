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

    @Value("${openai.model:gpt-4.1-mini}")
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

            String raw = webClient.post()
                    .uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(req))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return extractOutputText(raw);
        } catch (Exception e) {
            throw new RuntimeException("Report generation failed", e);
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
}


