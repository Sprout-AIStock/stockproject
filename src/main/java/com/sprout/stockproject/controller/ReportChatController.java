package com.sprout.stockproject.controller;

import com.sprout.stockproject.api.ApiResponse;
import com.sprout.stockproject.service.ReportChatLlmService;
import com.sprout.stockproject.service.report.ReportSearchService;
import com.sprout.stockproject.service.storage.ReportStorage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/chat")
public class ReportChatController {

    private final ReportStorage storage;
    private final ReportSearchService search;
    private final ReportChatLlmService llm;

    public ReportChatController(ReportStorage storage, ReportSearchService search, ReportChatLlmService llm) {
        this.storage = storage; this.search = search; this.llm = llm;
    }

    public record ChatReq(String question, Integer k) {}

    @PostMapping("/report")
    public ResponseEntity<ApiResponse<Map<String, Object>>> chat(@RequestBody ChatReq req) {
        String md = storage.loadLatest();
        if (md == null) {
            return ResponseEntity.status(409).body(ApiResponse.error("NO_REPORT", "먼저 리포트를 생성하세요"));
        }
        int k = req.k() == null ? 5 : Math.max(1, req.k());
        var snippets = search.topK(md, req.question(), k);
        List<Map<String, Object>> snippetMaps = new ArrayList<>();
        for (var s : snippets) {
            snippetMaps.add(Map.of(
                    "title", s.title(),
                    "snippet", s.snippet(),
                    "startLine", s.startLine(),
                    "endLine", s.endLine(),
                    "score", s.score()
            ));
        }
        var ans = llm.answer(req.question(), snippetMaps);
        Map<String,Object> body = new HashMap<>();
        body.put("answer", ans.answer());
        body.put("citations", ans.citations());
        body.put("usedReportDate", LocalDate.now().toString());
        return ResponseEntity.ok(ApiResponse.ok(body));
    }
}


