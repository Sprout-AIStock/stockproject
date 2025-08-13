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

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

@Component
public class NaverNewsClient {

    private final WebClient wc;
    private final ObjectMapper om;

    @Value("${api.naver.search.id:}")
    private String clientId;

    @Value("${api.naver.search.secret:}")
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

        // 키 확인 (환경변수 폴백 허용)
        String effectiveId = isBlank(clientId) ? System.getenv("API_NAVER_SEARCH_ID") : clientId;
        String effectiveSecret = isBlank(clientSecret) ? System.getenv("API_NAVER_SEARCH_SECRET") : clientSecret;
        if (isBlank(effectiveId) || isBlank(effectiveSecret)) {
            throw new IllegalStateException("Naver API key missing: set 'api.naver.search.id' and 'api.naver.search.secret'");
        }

        try {
            String raw = null;
            try {
                raw = wc.get()
                        .uri(u -> u.path("/v1/search/news.json")
                                .queryParam("query", query)
                                .queryParam("display", String.valueOf(disp))
                                .queryParam("start",   String.valueOf(st))
                                .queryParam("sort", "date")
                                .build())
                        .accept(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "Mozilla/5.0 (compatible; stockproject/1.0)")
                        .header("X-Naver-Client-Id", effectiveId)
                        .header("X-Naver-Client-Secret", effectiveSecret)
                        .retrieve()
                        .onStatus(HttpStatusCode::isError, res ->
                                res.bodyToMono(String.class).flatMap(msg ->
                                        reactor.core.publisher.Mono.error(new RuntimeException("Naver API error: " + msg))))
                        .bodyToMono(String.class)
                        .block();
            } catch (Exception primary) {
                // Fallback: JDK HttpClient (HTTP/1.1), 명시적 URL 인코딩
                String encoded = URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
                URI uri = URI.create("https://openapi.naver.com/v1/search/news.json?query=" + encoded +
                        "&display=" + disp + "&start=" + st + "&sort=date");
                HttpClient client = HttpClient.newBuilder()
                        .version(HttpClient.Version.HTTP_1_1)
                        .connectTimeout(Duration.ofSeconds(10))
                        .build();
                HttpRequest request = HttpRequest.newBuilder(uri)
                        .timeout(Duration.ofSeconds(30))
                        .header("Accept", "application/json")
                        .header("User-Agent", "Mozilla/5.0 (compatible; stockproject/1.0)")
                        .header("X-Naver-Client-Id", effectiveId)
                        .header("X-Naver-Client-Secret", effectiveSecret)
                        .GET()
                        .build();
                HttpResponse<String> resp = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (resp.statusCode() >= 400) {
                    throw new RuntimeException("Naver API error (fallback): status=" + resp.statusCode() + ", body=" + resp.body());
                }
                raw = resp.body();
            }

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