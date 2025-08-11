// src/main/java/com/sprout/stockproject/external/naver/NaverMobileStockClient.java
package com.sprout.stockproject.external;

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

    /** 종목명 추출 */
    public String getStockName(String stockCode) {
        JsonNode data = fetchIntegration(stockCode);
        return data.get("stockName").asText();
    }

    /** 현재가 추출 */
    public String getCurrentPrice(String stockCode) {
        JsonNode data = fetchIntegration(stockCode);
        JsonNode dealTrendInfos = data.get("dealTrendInfos");
        if (dealTrendInfos != null && dealTrendInfos.isArray() && dealTrendInfos.size() > 0) {
            return dealTrendInfos.get(0).get("closePrice").asText();
        }
        return null;
    }

    /** 시총 추출 */
    public String getMarketValue(String stockCode) {
        JsonNode data = fetchIntegration(stockCode);
        return findValueByCode(data, "marketValue");
    }

    /** PER 추출 */
    public String getPER(String stockCode) {
        JsonNode data = fetchIntegration(stockCode);
        return findValueByCode(data, "per");
    }

    /** PBR 추출 */
    public String getPBR(String stockCode) {
        JsonNode data = fetchIntegration(stockCode);
        return findValueByCode(data, "pbr");
    }

    /** totalInfos 배열에서 특정 code의 value 찾기 */
    private String findValueByCode(JsonNode data, String targetCode) {
        JsonNode totalInfos = data.get("totalInfos");
        if (totalInfos != null && totalInfos.isArray()) {
            for (JsonNode info : totalInfos) {
                if (targetCode.equals(info.get("code").asText())) {
                    return info.get("value").asText();
                }
            }
        }
        return null;
    }
}