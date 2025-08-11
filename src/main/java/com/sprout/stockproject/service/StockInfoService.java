package com.sprout.stockproject.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sprout.stockproject.api.UnprocessableException;
import com.sprout.stockproject.dto.StockDetailDto;
import com.sprout.stockproject.external.NaverMobileStockClient;
import org.springframework.stereotype.Service;

@Service
public class StockInfoService {

    private final NaverMobileStockClient naverMobile;

    public StockInfoService(NaverMobileStockClient naverMobile) {
        this.naverMobile = naverMobile;
    }

    public StockDetailDto fetch(String code) {
        JsonNode root = naverMobile.fetchIntegration(code);
        if (root == null || root.isEmpty()) {
            throw new UnprocessableException("종목 데이터를 찾을 수 없습니다: code=" + code);
        }
        String name = text(root, "stockNameKr");
        String price = text(root, "now");
        String marketCap = text(root, "marketCap");
        String per = text(root, "per");
        String pbr = text(root, "pbr");
        if (name == null || name.isBlank()) {
            throw new UnprocessableException("종목 코드가 유효하지 않습니다: code=" + code);
        }
        return new StockDetailDto(code, name, price, marketCap, per, pbr);
    }

    private String text(JsonNode root, String field) {
        JsonNode n = root.path(field);
        return n.isMissingNode() ? null : n.asText(null);
    }
}


