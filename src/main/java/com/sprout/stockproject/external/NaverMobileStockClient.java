package com.sprout.stockproject.external;// src/main/java/com/sprout/stockproject/external/naver/NaverMobileStockClient.java

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class NaverMobileStockClient {

    private final WebClient wc;
    private final ObjectMapper om;

    public NaverMobileStockClient(WebClient.Builder builder, ObjectMapper om) {
        this.wc = builder.baseUrl("https://m.stock.naver.com").build();
        this.om = om;
    }

    /** 종목 통합 정보 (원래 쓰던 /api/stock/{code}/integration) */
    public JsonNode fetchIntegration(String stockCode) {
        try {
            String raw = wc.get()
                    .uri(uri -> uri.path("/api/stock/{code}/integration").build(stockCode))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            return om.readTree(raw);
        } catch (Exception e) {
            throw new RuntimeException("NaverMobileStockClient.fetchIntegration failed: " + e.getMessage(), e);
        }
    }
}