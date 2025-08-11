package com.sprout.stockproject.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.sprout.stockproject.external.NaverMobileStockClient;
import com.sprout.stockproject.external.NaverChartStockClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    private final NaverMobileStockClient naverClient;
    private final NaverChartStockClient chartClient;

    @Autowired
    public StockController(NaverMobileStockClient naverClient, NaverChartStockClient chartClient) {
        this.naverClient = naverClient;
        this.chartClient = chartClient;
    }

    /** 종목 전체 정보 조회 */
    @GetMapping("/{stockCode}")
    public ResponseEntity<Map<String, Object>> getStockInfo(@PathVariable String stockCode) {
        try {
            Map<String, Object> result = new HashMap<>();
            result.put("stockCode", stockCode);
            result.put("stockName", naverClient.getStockName(stockCode));
            result.put("currentPrice", naverClient.getCurrentPrice(stockCode));
            result.put("marketValue", naverClient.getMarketValue(stockCode));
            result.put("per", naverClient.getPER(stockCode));
            result.put("pbr", naverClient.getPBR(stockCode));

            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Failed to fetch stock info: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /** 종목명만 조회 */
    @GetMapping("/{stockCode}/name")
    public ResponseEntity<Map<String, String>> getStockName(@PathVariable String stockCode) {
        try {
            Map<String, String> result = new HashMap<>();
            result.put("stockName", naverClient.getStockName(stockCode));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch stock name: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /** 현재가만 조회 */
    @GetMapping("/{stockCode}/price")
    public ResponseEntity<Map<String, String>> getCurrentPrice(@PathVariable String stockCode) {
        try {
            Map<String, String> result = new HashMap<>();
            result.put("currentPrice", naverClient.getCurrentPrice(stockCode));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch current price: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /** 시총만 조회 */
    @GetMapping("/{stockCode}/market-value")
    public ResponseEntity<Map<String, String>> getMarketValue(@PathVariable String stockCode) {
        try {
            Map<String, String> result = new HashMap<>();
            result.put("marketValue", naverClient.getMarketValue(stockCode));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch market value: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /** PER만 조회 */
    @GetMapping("/{stockCode}/per")
    public ResponseEntity<Map<String, String>> getPER(@PathVariable String stockCode) {
        try {
            Map<String, String> result = new HashMap<>();
            result.put("per", naverClient.getPER(stockCode));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch PER: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /** PBR만 조회 */
    @GetMapping("/{stockCode}/pbr")
    public ResponseEntity<Map<String, String>> getPBR(@PathVariable String stockCode) {
        try {
            Map<String, String> result = new HashMap<>();
            result.put("pbr", naverClient.getPBR(stockCode));
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to fetch PBR: " + e.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    /** 원본 네이버 데이터 조회 (디버깅용) */
    @GetMapping("/{stockCode}/raw")
    public ResponseEntity<JsonNode> getRawData(@PathVariable String stockCode) {
        try {
            JsonNode result = naverClient.fetchIntegration(stockCode);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

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
