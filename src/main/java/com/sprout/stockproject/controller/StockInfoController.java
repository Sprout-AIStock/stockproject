package com.sprout.stockproject.controller;

import com.sprout.stockproject.entity.StockInfo;
import com.sprout.stockproject.service.StockService;
import com.sprout.stockproject.service.StockInfoFromKrx;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/stocks")
public class StockInfoController {

    private final StockService stockService;
    private final StockInfoFromKrx stockInfoFromKrx;

    public StockInfoController(StockService stockService, StockInfoFromKrx stockInfoFromKrx) {
        this.stockService = stockService;
        this.stockInfoFromKrx = stockInfoFromKrx;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadCsv(@RequestParam("file") MultipartFile file) {
        try {
            String result = stockService.processCsvFile(file);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("업로드 실패: " + e.getMessage());
        }
    }

    @GetMapping("/search")
    public ResponseEntity<StockInfo> searchStock(@RequestParam("name") String stockName) {
        Optional<StockInfo> stock = stockService.searchByStockName(stockName);

        if (stock.isPresent()) {
            return ResponseEntity.ok(stock.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/search/keyword")
    public ResponseEntity<List<StockInfo>> searchByKeyword(@RequestParam("q") String keyword) {
        List<StockInfo> stocks = stockService.searchByKeyword(keyword);
        return ResponseEntity.ok(stocks);
    }
}
