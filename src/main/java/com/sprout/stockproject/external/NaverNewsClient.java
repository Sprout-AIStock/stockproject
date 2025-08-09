// src/main/java/com/sprout/stockproject/external/NaverNewsClient.java
package com.sprout.stockproject.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;

@Component
public class NaverNewsClient {

    private final WebClient wc;
    private final ObjectMapper om;

    @Value("${api.naver.search.id}")
    private String clientId;

    @Value("${api.naver.search.secret}")
    private String clientSecret;

    public NaverNewsClient(WebClient.Builder base, ObjectMapper om) {
        // 공용 Builder를 복제해 네이버 전용 세팅만 추가
        this.wc = base.clone()
                .baseUrl("https://openapi.naver.com")
                .filter(ExchangeFilterFunction.ofRequestProcessor(req ->
                        reactor.core.publisher.Mono.fromRunnable(() ->
                                System.out.println("[WebClient][NAVER] " + req.method() + " " + req.url()))))
                .build();
        this.om = om;
    }

    /**
     * 네이버 뉴스 검색 (정렬: date)
     * @param query   검색어
     * @param display 1~100
     * @param start   1 이상
     */
    public List<Map<String, Object>> searchNews(String query, int display, int start) {
        // 파라미터 보정 후 final 로컬 변수로 사용 (람다 캡처 안전)
        final int disp = Math.min(Math.max(display, 1), 100);
        final int st   = Math.max(start, 1);

        // 키 확인
        if (isBlank(clientId) || isBlank(clientSecret)) {
            throw new IllegalStateException("Naver API key missing: set 'api.naver.search.id' and 'api.naver.search.secret'");
        }

        try {
            String raw = wc.get()
                    .uri(u -> u.path("/v1/search/news.json")
                            .queryParam("query", query)
                            .queryParam("display", String.valueOf(disp)) // 모호성 제거
                            .queryParam("start",   String.valueOf(st))   // 모호성 제거
                            .queryParam("sort", "date")
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .header("X-Naver-Client-Id", clientId)
                    .header("X-Naver-Client-Secret", clientSecret)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, res ->
                            res.bodyToMono(String.class).flatMap(msg ->
                                    reactor.core.publisher.Mono.error(new RuntimeException("Naver API error: " + msg))))
                    .bodyToMono(String.class)
                    .block();

            if (raw == null || raw.isBlank()) return Collections.emptyList();

            JsonNode root = om.readTree(raw);
            JsonNode items = root.path("items");
            List<Map<String, Object>> out = new ArrayList<>();
            if (items.isArray()) {
                for (JsonNode it : items) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("title",       unescapeBasic(stripTags(it.path("title").asText(""))));
                    m.put("originallink",it.path("originallink").asText(""));
                    m.put("link",        it.path("link").asText(""));
                    m.put("description", unescapeBasic(stripTags(it.path("description").asText(""))));
                    m.put("pubDate",     it.path("pubDate").asText(""));
                    out.add(m);
                }
            }
            return out;
        } catch (Exception e) {
            throw new RuntimeException("NaverNewsClient.searchNews failed: " + e.getMessage(), e);
        }
    }

    // --- helpers ---

    private boolean isBlank(String s) { return s == null || s.isBlank(); }

    /** 아주 단순한 태그 제거 (ex. <b> … </b>) */
    private String stripTags(String s) {
        if (s == null || s.isBlank()) return "";
        return s.replaceAll("<[^>]*>", "");
    }

    /** 기본 HTML 엔티티만 언이스케이프 (외부 라이브러리 없이 최소치) */
    private String unescapeBasic(String s) {
        if (s == null) return "";
        return s.replace("&quot;", "\"")
                .replace("&#34;", "\"")
                .replace("&apos;", "'")
                .replace("&#39;", "'")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">");
    }
}