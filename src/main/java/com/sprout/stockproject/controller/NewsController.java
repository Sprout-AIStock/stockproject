// src/main/java/com/sprout/stockproject/controller/NewsController.java
package com.sprout.stockproject.controller;

import com.sprout.stockproject.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*; // 👈 RequestParam 추가

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsService newsService;

    @Autowired
    public NewsController(NewsService newsService) {
        this.newsService = newsService;
    }

    @GetMapping("/theme/{themeName}")
    // 👇 RequestParam으로 display, start 값을 받도록 수정
    public ResponseEntity<List<Map<String, Object>>> getNewsByTheme(
            @PathVariable String themeName,
            @RequestParam(required = false, defaultValue = "10") int display,
            @RequestParam(required = false, defaultValue = "1") int start
    ) {
        List<Map<String, Object>> newsList = newsService.searchNews(themeName, display, start);
        if (newsList == null || newsList.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(newsList);
    }

    @GetMapping("/macro")
    // 👇 RequestParam으로 display, start 값을 받도록 수정
    public ResponseEntity<List<Map<String, Object>>> getMacroNews(
            @RequestParam(required = false, defaultValue = "10") int display,
            @RequestParam(required = false, defaultValue = "1") int start
    ) {
        List<Map<String, Object>> newsList = newsService.searchNews("거시경제", display, start);
        if (newsList == null || newsList.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(newsList);
    }
}