// src/main/java/com/sprout/stockproject/dto/NewsArticleDto.java
package com.sprout.stockproject.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NewsArticleDto {
    private String title;
    private String originallink;
    private String link;
    private String description;
    private String pubDate;
}