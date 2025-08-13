package com.sprout.stockproject.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sprout.stockproject.external.NaverChartStockClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final NaverChartStockClient chartClient;

    @Autowired
    public StockController(NaverChartStockClient chartClient) {
        this.chartClient = chartClient;
    }

    /** 종목 전체 정보 조회 (차트 기반 최소 정보 제공) */
    @GetMapping("/{stockCode}")
    public ResponseEntity<Map<String, Object>> getStockInfo(@PathVariable String stockCode) {
        try {
            JsonNode chart = chartClient.fetchDailyChart(stockCode, 1);
            Map<String, Object> result = new HashMap<>();
            result.put("stockCode", stockCode);
            result.put("stockName", chart.path("name").asText(""));
            String price = null;
            JsonNode data = chart.path("data");
            if (data != null && data.isArray() && data.size() > 0) {
                JsonNode last = data.get(data.size()-1);
                price = last.path("close").asText(null);
            }
            result.put("currentPrice", price);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch stock info: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /** 종목명만 조회 (차트에서 이름 사용) */
    @GetMapping("/{stockCode}/name")
    public ResponseEntity<Map<String, String>> getStockName(@PathVariable String stockCode) {
        try {
            JsonNode chart = chartClient.fetchDailyChart(stockCode, 1);
            Map<String, String> result = new HashMap<>();
            result.put("stockName", chart.path("name").asText(""));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch stock name: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /** 현재가만 조회 (차트 마지막 종가 사용) */
    @GetMapping("/{stockCode}/price")
    public ResponseEntity<Map<String, String>> getCurrentPrice(@PathVariable String stockCode) {
        try {
            JsonNode chart = chartClient.fetchDailyChart(stockCode, 1);
            String price = null;
            JsonNode data = chart.path("data");
            if (data != null && data.isArray() && data.size() > 0) {
                JsonNode last = data.get(data.size()-1);
                price = last.path("close").asText(null);
            }
            Map<String, String> result = new HashMap<>();
            result.put("currentPrice", price);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch current price: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    // market-value / per / pbr 엔드포인트는 비활성(데이터 소스 제거)

    // per/pbr/raw 엔드포인트 제거(네이버 모바일 통합 API 의존성 제거)

    /** 일봉 차트 데이터 조회 */
    @GetMapping("/{stockCode}/chart/daily")
    public ResponseEntity<JsonNode> getDailyChart(@PathVariable String stockCode,
                                                  @RequestParam(defaultValue = "30") int count) {
        try {
            JsonNode result = chartClient.fetchDailyChart(stockCode, count);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /** 주봉 차트 데이터 조회 */
    @GetMapping("/{stockCode}/chart/weekly")
    public ResponseEntity<JsonNode> getWeeklyChart(@PathVariable String stockCode,
                                                   @RequestParam(defaultValue = "20") int count) {
        try {
            JsonNode result = chartClient.fetchWeeklyChart(stockCode, count);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /** 월봉 차트 데이터 조회 */
    @GetMapping("/{stockCode}/chart/monthly")
    public ResponseEntity<JsonNode> getMonthlyChart(@PathVariable String stockCode,
                                                    @RequestParam(defaultValue = "12") int count) {
        try {
            JsonNode result = chartClient.fetchMonthlyChart(stockCode, count);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
