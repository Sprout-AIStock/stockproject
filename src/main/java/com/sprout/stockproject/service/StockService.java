package com.sprout.stockproject.service;

import com.sprout.stockproject.entity.StockInfo;
import com.sprout.stockproject.repository.StockInfoRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
public class StockService {

    private final StockInfoRepository stockRepository;

    public StockService(StockInfoRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public long getStockCount() {
        return stockRepository.count();
    }

    public List<StockInfo> getAllStocks() {
        return stockRepository.findAll();
    }

    public Optional<StockInfo> searchByStockName(String stockName) {
        return stockRepository.findByStockName(stockName);
    }

    public List<StockInfo> searchByKeyword(String keyword) {
        return stockRepository.findByStockNameContaining(keyword);
    }

    public String processCsvFile(MultipartFile file) throws IOException {
        int processedCount = 0;
        int duplicateCount = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // 헤더 스킵
                }

                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String stockName = parts[0].trim().replaceAll("\"", "");
                    String stockCode = parts[1].trim().replaceAll("\"", "");

                    if (!stockRepository.existsByStockCode(stockCode)) {
                        StockInfo stockInfo = new StockInfo();
                        stockInfo.setStockName(stockName);
                        stockInfo.setStockCode(stockCode);
                        stockRepository.save(stockInfo);
                        processedCount++;
                    } else {
                        duplicateCount++;
                    }
                }
            }
        }

        return String.format("처리 완료: %d개 추가, %d개 중복", processedCount, duplicateCount);
    }

    public String loadFromServerFile(String filename) throws IOException {
        Path filePath = Paths.get(filename);

        if (!Files.exists(filePath)) {
            throw new IOException("파일을 찾을 수 없습니다: " + filename);
        }

        int processedCount = 0;
        int duplicateCount = 0;

        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // 헤더 스킵
                }

                String[] parts = line.split(",");
                if (parts.length >= 2) {
                    String stockName = parts[0].trim().replaceAll("\"", "");
                    String stockCode = parts[1].trim().replaceAll("\"", "");

                    if (!stockRepository.existsByStockCode(stockCode)) {
                        StockInfo stockInfo = new StockInfo();
                        stockInfo.setStockName(stockName);
                        stockInfo.setStockCode(stockCode);
                        stockRepository.save(stockInfo);
                        processedCount++;
                    } else {
                        duplicateCount++;
                    }
                }
            }
        }

        return String.format("서버 파일 처리 완료: %d개 추가, %d개 중복", processedCount, duplicateCount);
    }
}
