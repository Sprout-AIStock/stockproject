// src/main/java/com/sprout/stockproject/service/NewsService.java
package com.sprout.stockproject.service;

import com.sprout.stockproject.external.NaverNewsClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;


@Service
public class NewsService {

    private final NaverNewsClient naverNewsClient;

    public NewsService(NaverNewsClient naverNewsClient) {
        this.naverNewsClient = naverNewsClient;
    }

    public List<Map<String, Object>> searchNews(String keyword, int display, int start) {
        return naverNewsClient.searchNews(keyword, display, start);
    }
}
