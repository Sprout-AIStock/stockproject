package com.sprout.stockproject.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor // 모든 필드를 인자로 받는 생성자를 만듭니다.
public class StockDetailDto {
    private String code;
    private String name;
    private String price;
    private String marketCap;
    private String per;
    private String pbr;
}