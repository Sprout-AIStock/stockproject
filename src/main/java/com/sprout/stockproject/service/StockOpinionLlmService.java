package com.sprout.stockproject.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprout.stockproject.dto.MacroQuadResponse;
import com.sprout.stockproject.dto.StockDetailDto;
import com.sprout.stockproject.prompt.PromptStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StockOpinionLlmService {

    private final PromptStore prompts;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Value("${prompt.stock.opinion.version:stock-opinion-v1}")
    private String promptName;

    @Value("${openai.model:gpt-5}")
    private String model;

    public StockOpinionLlmService(PromptStore prompts,
                                  ObjectMapper objectMapper,
                                  @Qualifier("openaiWebClient") WebClient openaiWebClient) {
        this.prompts = prompts;
        this.objectMapper = objectMapper;
        this.webClient = openaiWebClient;
    }

    public OpinionResult infer(MacroQuadResponse macro, StockDetailDto stock, String horizon, String risk) {
        try {
            Map<String, Object> input = new HashMap<>();
            input.put("macro", macro);
            input.put("stock", Map.of(
                    "code", stock.getCode(),
                    "name", stock.getName(),
                    "price", stock.getPrice(),
                    "marketCap", stock.getMarketCap(),
                    "per", stock.getPer(),
                    "pbr", stock.getPbr()
            ));
            input.put("horizon", horizon);
            input.put("risk", risk);

            String inputJson = objectMapper.writeValueAsString(input);
            String base = prompts.get(promptName);
            String prompt = base.replace("{{INPUT_JSON}}", inputJson);

            Map<String, Object> req = new HashMap<>();
            req.put("model", model);
            req.put("input", prompt);
            req.put("temperature", 0);
            req.put("max_output_tokens", 400);
            Map<String, Object> respFmt = new HashMap<>();
            respFmt.put("type", "json_schema");
            respFmt.put("json_schema", Map.of("name", "stock_opinion", "schema", responseSchema()));
            req.put("response_format", respFmt);

            String raw = webClient.post()
                    .uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(objectMapper.writeValueAsString(req))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            String json = extractOutputJson(raw);
            JsonNode node = objectMapper.readTree(json);
            String stance = node.path("stance").asText("neutral");
            double confidence = node.path("confidence").asDouble(0.2);
            List<String> reasons = objectMapper.convertValue(node.path("reasons"), objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            String asOf = node.path("asOf").asText(macro.asOf());
            return new OpinionResult(stance, confidence, reasons, asOf);
        } catch (Exception e) {
            throw new RuntimeException("Stock opinion inference failed", e);
        }
    }

    private Map<String, Object> responseSchema() {
        return Map.of(
                "$schema", "http://json-schema.org/draft-07/schema#",
                "type", "object",
                "additionalProperties", false,
                "properties", Map.of(
                        "stance", Map.of("type", "string", "enum", new String[]{"buy", "neutral", "sell"}),
                        "confidence", Map.of("type", "number", "minimum", 0, "maximum", 1),
                        "reasons", Map.of("type", "array", "items", Map.of("type", "string")),
                        "asOf", Map.of("type", "string")
                ),
                "required", new String[]{"stance", "confidence", "reasons", "asOf"}
        );
    }

    private String extractOutputJson(String raw) throws Exception {
        JsonNode root = objectMapper.readTree(raw);
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
        return raw;
    }

    public record OpinionResult(String stance, double confidence, List<String> reasons, String asOf) {}
}


