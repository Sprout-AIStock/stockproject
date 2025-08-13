// src/main/java/com/sprout/stockproject/external/naver/NaverMobileStockClient.java
package com.sprout.stockproject.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class NaverMobileStockClient {

    private final WebClient wc;
    private final ObjectMapper om;
    private final java.util.concurrent.ConcurrentHashMap<String, CacheEntry> cache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final long TTL_MILLIS = 5 * 60 * 1000; // 5분 캐시

    public NaverMobileStockClient(WebClient.Builder builder, ObjectMapper om) {
        this.wc = builder.baseUrl("https://m.stock.naver.com").build();
        this.om = om;
    }

    /** 종목 통합 정보 (원래 쓰던 /api/stock/{code}/integration) */
    public JsonNode fetchIntegration(String stockCode) {
        String lastError = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                String path = "/api/stock/" + stockCode + "/integration";
                String raw = wc.get()
                        .uri(path)
                        .accept(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 14_0 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/14.0 Mobile/15E148 Safari/604.1")
                        .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                        .header("Referer", "https://m.stock.naver.com/")
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                if (raw == null || raw.isBlank()) throw new RuntimeException("Empty response body");
                JsonNode parsed = om.readTree(raw);
                cache.put(stockCode, new CacheEntry(parsed, System.currentTimeMillis()));
                return parsed;
            } catch (WebClientResponseException wce) {
                lastError = wce.getStatusCode() + " " + wce.getResponseBodyAsString();
                // 409/429/5xx 재시도
                int code = wce.getStatusCode().value();
                if (code == 409 || code == 429 || code >= 500) {
                    sleepBackoff(attempt);
                    // 캐시가 있으면 즉시 반환 (소극적 폴백)
                    CacheEntry ce = cache.get(stockCode);
                    if (ce != null && !ce.isExpired()) return ce.data();
                    continue;
                }
                throw new RuntimeException("NaverMobileStockClient.fetchIntegration failed: " + wce.getMessage(), wce);
            } catch (Exception e) {
                lastError = e.getMessage();
                sleepBackoff(attempt);
                CacheEntry ce = cache.get(stockCode);
                if (ce != null && !ce.isExpired()) return ce.data();
            }
        }
        CacheEntry ce = cache.get(stockCode);
        if (ce != null && !ce.isExpired()) return ce.data();
        throw new RuntimeException("NaverMobileStockClient.fetchIntegration failed after retries: " + lastError);
    }

    private void sleepBackoff(int attempt) {
        try {
            long[] waits = {0L, 300L, 800L, 1500L};
            long ms = attempt < waits.length ? waits[attempt] : 1500L;
            if (ms > 0) Thread.sleep(ms);
        } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); }
    }

    private record CacheEntry(JsonNode data, long storedAt) {
        boolean isExpired() { return System.currentTimeMillis() - storedAt > TTL_MILLIS; }
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