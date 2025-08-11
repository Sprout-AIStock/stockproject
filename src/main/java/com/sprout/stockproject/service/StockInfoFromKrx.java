// src/main/java/com/sprout/stockproject/service/StockInfoFromKrx.java
package com.sprout.stockproject.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sprout.stockproject.entity.StockInfo;
import com.sprout.stockproject.repository.StockInfoRepository;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class StockInfoFromKrx {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final StockInfoRepository stockRepository;

    public StockInfoFromKrx(StockInfoRepository stockRepository) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.stockRepository = stockRepository;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        System.out.println("=================================");
        System.out.println("자동으로 KRX 데이터 다운로드를 시작합니다...");
        System.out.println("=================================");

        try {
            String result = downloadFromKrx();
            System.out.println("자동 다운로드 결과: " + result);
        } catch (Exception e) {
            System.err.println("서버 시작 시 자동 다운로드 실패: " + e.getMessage());
        }
    }

    public String downloadFromKrx() {
        try {
            // KRX 종목정보 다운로드 URL
            String url = "http://data.krx.co.kr/comm/bldAttendant/getJsonData.cmd";

            // 현재 날짜 (YYYYMMDD 형식)
            String searchDate = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            // POST 요청 파라미터 설정
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("bld", "dbms/MDC/STAT/standard/MDCSTAT01501");
            params.add("locale", "ko_KR");
            params.add("mktId", "ALL"); // 전체 시장
            params.add("trdDd", searchDate);
            params.add("money", "1");
            params.add("csvxls_isNo", "false");

            // 헤더 설정 - KRX 요청 시 필요한 헤더들
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            headers.add("Referer", "http://data.krx.co.kr/contents/MDC/MDI/mdiLoader/index.cmd?menuId=MDC0201");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            // KRX API 호출
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String jsonData = response.getBody();
                System.out.println("KRX 데이터 다운로드 성공!");
                System.out.println("응답 데이터 크기: " + (jsonData != null ? jsonData.length() : 0) + " 문자");

                // JSON 데이터 파싱하여 데이터베이스에 저장
                int savedCount = parseAndSaveData(jsonData);

                return String.format("KRX 데이터 다운로드 및 저장 성공 - 데이터 크기: %d 문자, 저장된 종목 수: %d개",
                        (jsonData != null ? jsonData.length() : 0), savedCount);
            } else {
                return "KRX 데이터 다운로드 실패 - HTTP 상태: " + response.getStatusCode();
            }

        } catch (Exception e) {
            System.err.println("KRX 다운로드 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return "KRX 다운로드 실패: " + e.getMessage();
        }
    }

    private int parseAndSaveData(String jsonData) {
        try {
            JsonNode rootNode = objectMapper.readTree(jsonData);
            JsonNode outBlock = rootNode.get("OutBlock_1");

            if (outBlock != null && outBlock.isArray()) {
                int savedCount = 0;
                int duplicateCount = 0;

                for (JsonNode stockNode : outBlock) {
                    String stockCode = stockNode.get("ISU_SRT_CD").asText(); // 단축코드
                    String stockName = stockNode.get("ISU_ABBRV").asText(); // 종목명 약어

                    // 종목코드가 유효한지 확인
                    if (stockCode != null && !stockCode.trim().isEmpty() &&
                            stockName != null && !stockName.trim().isEmpty()) {

                        // 중복 체크
                        if (!stockRepository.existsByStockCode(stockCode)) {
                            StockInfo stockInfo = new StockInfo();
                            stockInfo.setStockCode(stockCode);
                            stockInfo.setStockName(stockName);
                            stockRepository.save(stockInfo);
                            savedCount++;
                        } else {
                            duplicateCount++;
                        }
                    }
                }

                System.out.println(String.format("데이터 처리 완료: %d개 저장, %d개 중복", savedCount, duplicateCount));
                return savedCount;
            }

            return 0;
        } catch (Exception e) {
            System.err.println("JSON 파싱 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            return 0;
        }
    }
}
