// src/main/java/com/sprout/stockproject/service/NewsService.java
package com.sprout.stockproject.service;

// ... (import 구문은 기존과 동일) ...

import lombok.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class NewsService {

    @Value("${api.naver.search.id}")
    private String naverClientId;

    @Value("${api.naver.search.secret}")
    private String naverClientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    // 👇 display, start 파라미터를 받도록 수정
    public List<Map<String, Object>> searchNews(String keyword, int display, int start) {
        // 👇 파라미터를 URL에 반영
        String url = "https://openapi.naver.com/v1/search/news.json?query=" + keyword +
                "&display=" + display + "&start=" + start + "&sort=date";

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", naverClientId);
        headers.set("X-Naver-Client-Secret", naverClientSecret);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null) {
                return (List<Map<String, Object>>) response.getBody().get("items");
            }
        } catch (Exception e) {
            System.out.println("Naver API 호출 오류: " + e.getMessage());
        }
        return null;
    }
}