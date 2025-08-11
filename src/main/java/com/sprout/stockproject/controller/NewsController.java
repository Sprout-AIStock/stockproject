package com.sprout.stockproject.controller;

import com.sprout.stockproject.api.ApiResponse;
import com.sprout.stockproject.service.NewsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;

    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/theme/{themeName}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getNewsByTheme(
            @PathVariable String themeName,
            @RequestParam(required = false, defaultValue = "10") int display,
            @RequestParam(required = false, defaultValue = "1") int start
    ) {
        List<Map<String, Object>> newsList = newsService.searchNews(themeName, display, start);
        return ResponseEntity.ok(ApiResponse.ok(newsList)); // 빈 배열도 200으로 반환
    }

    @GetMapping("/macro")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMacroNews(
            @RequestParam(required = false, defaultValue = "10") int display,
            @RequestParam(required = false, defaultValue = "1") int start
    ) {
        List<Map<String, Object>> newsList = newsService.searchNews("거시경제", display, start);
        return ResponseEntity.ok(ApiResponse.ok(newsList)); // 빈 배열도 200으로 반환
    }
}