package com.sprout.stockproject.controller;

import com.sprout.stockproject.api.ApiResponse;
import com.sprout.stockproject.cache.MacroSnapshotCache;
import com.sprout.stockproject.dto.MacroQuadInput;
import com.sprout.stockproject.service.MacroQuadLlmService;
import com.sprout.stockproject.service.ReportService;
import com.sprout.stockproject.service.storage.ReportStorage;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/report")
public class ReportController {

    private final MacroSnapshotCache cache;
    private final MacroQuadLlmService macro;
    private final ReportService reports;
    private final ReportStorage storage;

    public ReportController(MacroSnapshotCache cache, MacroQuadLlmService macro, ReportService reports, ReportStorage storage) {
        this.cache = cache; this.macro = macro; this.reports = reports; this.storage = storage;
    }

    @PostMapping("/daily")
    public ResponseEntity<ApiResponse<Map<String, Object>>> daily(@RequestParam(required = false, defaultValue = "false") boolean force) {
        Optional<MacroQuadInput> opt = cache.latest();
        if (opt.isEmpty()) return ResponseEntity.status(409).body(ApiResponse.error("NO_REPORT_INPUT", "먼저 거시 스냅샷을 준비하세요"));
        var in = opt.get();
        var quad = macro.inferSafe(in);
        String md = reports.generateDailyReport(in, quad);
        String path = storage.saveToday(md);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "date", LocalDate.now().toString().replace("-", ""),
                "size", md.length(),
                "path", path
        )));
    }

    @GetMapping(value = "/latest")
    public ResponseEntity<?> latest(@RequestParam(required = false, defaultValue = "md") String format) {
        String md = storage.loadLatest();
        if (md == null) return ResponseEntity.status(404).body(ApiResponse.error("REPORT_NOT_FOUND", "리포트가 없습니다"));
        if ("html".equalsIgnoreCase(format)) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(markdownToHtml(md));
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(md);
    }

    @GetMapping(value = "/{date}")
    public ResponseEntity<?> byDate(@PathVariable("date") String yyyymmdd, @RequestParam(required = false, defaultValue = "md") String format) {
        String md = storage.loadByDate(yyyymmdd);
        if (md == null) return ResponseEntity.status(404).body(ApiResponse.error("REPORT_NOT_FOUND", "리포트가 없습니다"));
        if ("html".equalsIgnoreCase(format)) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_HTML).body(markdownToHtml(md));
        }
        return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body(md);
    }

    private String markdownToHtml(String md) {
        // 간단 변환: 헤더만 처리(의존성 없이 임시)
        String html = md
                .replaceAll("(?m)^# (.*)$", "<h1>$1</h1>")
                .replaceAll("(?m)^## (.*)$", "<h2>$1</h2>")
                .replaceAll("(?m)^- (.*)$", "<li>$1</li>");
        return "<html><body>" + html + "</body></html>";
    }
}


