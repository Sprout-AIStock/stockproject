package com.sprout.stockproject.controller;

import com.sprout.stockproject.api.ApiResponse;
import com.sprout.stockproject.cache.MacroSnapshotCache;
import com.sprout.stockproject.dto.MacroQuadInput;
import com.sprout.stockproject.service.ReportChatLlmService;
import com.sprout.stockproject.service.StockInfoService;
import com.sprout.stockproject.service.StockOpinionService;
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
    private final MacroSnapshotCache cache;
    private final StockInfoService stockInfoService;
    private final StockOpinionService stockOpinionService;

    public ReportChatController(ReportStorage storage, ReportSearchService search, ReportChatLlmService llm,
                               MacroSnapshotCache cache, StockInfoService stockInfoService, StockOpinionService stockOpinionService) {
        this.storage = storage; this.search = search; this.llm = llm;
        this.cache = cache; this.stockInfoService = stockInfoService; this.stockOpinionService = stockOpinionService;
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

    public record StockChatReq(String question, String stockCode) {}

    @PostMapping("/stock")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stockChat(@RequestBody StockChatReq req) {
        Optional<MacroQuadInput> opt = cache.latest();
        if (opt.isEmpty()) {
            return ResponseEntity.status(409).body(ApiResponse.error("NO_MACRO_DATA", "거시 데이터가 준비되지 않았습니다"));
        }
        
        // 주식 코드 추출 시도
        String stockCode = req.stockCode();
        if (stockCode == null || stockCode.isBlank()) {
            stockCode = extractStockCode(req.question());
        }
        
        if (stockCode == null || stockCode.isBlank()) {
            Map<String, Object> body = new HashMap<>();
            body.put("answer", "주식 코드를 명확히 해주세요. 예: 삼성전자(005930), LG화학(051910) 등");
            body.put("suggestions", List.of("005930 (삼성전자)", "000660 (SK하이닉스)", "051910 (LG화학)"));
            return ResponseEntity.ok(ApiResponse.ok(body));
        }

        try {
            var stock = stockInfoService.fetch(stockCode);
            var opinion = stockOpinionService.buildOpinion(opt.get(), stock, "mid", "normal");
            
            Map<String, Object> body = new HashMap<>();
            body.put("answer", formatStockOpinion(stock.getName(), opinion));
            body.put("stockInfo", Map.of(
                "code", stock.getCode(),
                "name", stock.getName(),
                "price", stock.getPrice(),
                "marketCap", stock.getMarketCap(),
                "per", stock.getPer(),
                "pbr", stock.getPbr()
            ));
            body.put("opinion", opinion);
            return ResponseEntity.ok(ApiResponse.ok(body));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(ApiResponse.error("STOCK_ERROR", "주식 정보를 가져오는데 실패했습니다: " + e.getMessage()));
        }
    }

    private String extractStockCode(String question) {
        // 간단한 주식 코드 추출 로직
        if (question.contains("삼성전자") || question.contains("005930")) return "005930";
        if (question.contains("SK하이닉스") || question.contains("000660")) return "000660";
        if (question.contains("LG화학") || question.contains("051910")) return "051910";
        if (question.contains("네이버") || question.contains("035420")) return "035420";
        if (question.contains("카카오") || question.contains("035720")) return "035720";
        // 6자리 숫자 패턴 찾기
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("\\b\\d{6}\\b");
        java.util.regex.Matcher matcher = pattern.matcher(question);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private String formatStockOpinion(String stockName, Map<String, Object> opinion) {
        String stance = (String) opinion.get("stance");
        Double confidence = (Double) opinion.get("confidence");
        @SuppressWarnings("unchecked")
        List<String> reasons = (List<String>) opinion.get("reasons");
        
        // 더 자연스럽고 대화체 응답 생성
        StringBuilder sb = new StringBuilder();
        
        // 인사말로 시작
        sb.append(String.format("%s에 대해 분석해드렸습니다!\n\n", stockName));
        
        // 투자 의견을 자연스럽게 표현
        if ("buy".equalsIgnoreCase(stance)) {
            sb.append(String.format("현재 상황을 종합해보면 **매수** 관점에서 긍정적으로 보고 있어요. (분석 확신도: %.1f%%)\n\n", confidence * 100));
        } else if ("sell".equalsIgnoreCase(stance)) {
            sb.append(String.format("지금은 **매도** 쪽으로 판단하는 것이 좋을 것 같습니다. (분석 확신도: %.1f%%)\n\n", confidence * 100));
        } else {
            sb.append(String.format("현재로서는 **중립적인 관점**이 적절해 보입니다. (분석 확신도: %.1f%%)\n\n", confidence * 100));
        }
        
        // 이유를 자연스럽게 설명
        if (reasons != null && !reasons.isEmpty()) {
            sb.append("주요 판단 근거를 말씀드리면:\n\n");
            for (String reason : reasons) {
                sb.append("• ").append(reason).append("\n");
            }
            sb.append("\n");
        }
        
        // 마무리 멘트
        if ("buy".equalsIgnoreCase(stance)) {
            sb.append("물론 투자는 개인의 판단이 가장 중요하니, 추가적인 정보도 참고해서 신중하게 결정하시길 바라요! 📈");
        } else if ("sell".equalsIgnoreCase(stance)) {
            sb.append("다만 시장 상황은 계속 변하니까 지속적인 모니터링이 필요할 것 같아요. 📊");
        } else {
            sb.append("앞으로 상황 변화를 좀 더 지켜본 후에 투자 방향을 정하시는 게 어떨까요? 🤔");
        }
        
        return sb.toString();
    }
}


