package com.sprout.stockproject.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class ReportChatLlmService {

    private final PromptStore prompts;
    private final ObjectMapper om;
    private final WebClient wc;

    @Value("${prompt.report.qa.version:report-qa-v1}")
    private String promptName;
    @Value("${openai.model:gpt-4.1-mini}")
    private String model;

    public ReportChatLlmService(PromptStore prompts, ObjectMapper om, @Qualifier("openaiWebClient") WebClient wc) {
        this.prompts = prompts; this.om = om; this.wc = wc;
    }

    public QaResult answer(String question, List<Map<String, Object>> snippets) {
        try {
            Map<String, Object> input = new HashMap<>();
            input.put("question", question);
            input.put("snippets", snippets);
            String inputJson = om.writeValueAsString(input);
            String prompt = prompts.get(promptName).replace("{{INPUT_JSON}}", inputJson);

            Map<String, Object> req = new HashMap<>();
            req.put("model", model);
            req.put("input", prompt);
            req.put("temperature", 0);
            req.put("max_output_tokens", 600);
            Map<String, Object> respFmt = new HashMap<>();
            respFmt.put("type", "json_schema");
            respFmt.put("json_schema", Map.of("name", "report_qa", "schema", schema()));
            req.put("response_format", respFmt);

            String raw = wc.post().uri("/responses")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(om.writeValueAsString(req))
                    .retrieve().bodyToMono(String.class).block();

            String json = extractOutputJson(raw);
            JsonNode n = om.readTree(json);
            String answer = n.path("answer").asText("");
            List<Map<String,Object>> citations = om.convertValue(n.path("citations"), om.getTypeFactory().constructCollectionType(List.class, Map.class));
            return new QaResult(answer, citations);
        } catch (Exception e) {
            return new QaResult("리포트에 해당 근거 없음 또는 일시적 오류", List.of());
        }
    }

    private Map<String, Object> schema() {
        return Map.of(
                "$schema","http://json-schema.org/draft-07/schema#",
                "type","object",
                "additionalProperties", false,
                "properties", Map.of(
                        "answer", Map.of("type","string"),
                        "citations", Map.of("type","array","items", Map.of(
                                "type","object",
                                "properties", Map.of(
                                        "title", Map.of("type","string"),
                                        "startLine", Map.of("type","integer"),
                                        "endLine", Map.of("type","integer"),
                                        "score", Map.of("type","number")
                                ),
                                "required", new String[]{"title","startLine","endLine","score"}
                        ))
                ),
                "required", new String[]{"answer","citations"}
        );
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
        return raw;
    }

    public record QaResult(String answer, List<Map<String,Object>> citations) {}
}


