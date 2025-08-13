package com.sprout.stockproject.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.sprout.stockproject.api.UnprocessableException;
import com.sprout.stockproject.dto.StockDetailDto;
import com.sprout.stockproject.external.NaverMobileStockClient;
import com.sprout.stockproject.external.NaverChartStockClient;
import org.springframework.stereotype.Service;

@Service
public class StockInfoService {

    private final NaverMobileStockClient naverMobile;
    private final NaverChartStockClient chartClient;

    public StockInfoService(NaverMobileStockClient naverMobile, NaverChartStockClient chartClient) {
        this.naverMobile = naverMobile;
        this.chartClient = chartClient;
    }

    public StockDetailDto fetch(String code) {
        try {
            // NaverMobile API로 상세 정보 가져오기 시도
            String name = naverMobile.getStockName(code);
            String price = naverMobile.getCurrentPrice(code);
            String marketCap = naverMobile.getMarketValue(code);
            String per = naverMobile.getPER(code);
            String pbr = naverMobile.getPBR(code);
            
            if (name == null) {
                throw new UnprocessableException("종목 기본 정보를 가져오지 못했습니다(잠시 후 재시도): code=" + code);
            }
            return new StockDetailDto(code, name, price, marketCap, per, pbr);
        } catch (Exception e) {
            // 폴백: 차트 API에서 최소 정보만 구성
            JsonNode chart = chartClient.fetchDailyChart(code, 1);
            String name = chart.path("name").asText(null);
            String price = null;
            JsonNode data = chart.path("data");
            if (data != null && data.isArray() && data.size() > 0) {
                JsonNode last = data.get(data.size() - 1);
                price = last.path("close").asText(null);
            }
            if (name == null) {
                throw new UnprocessableException("종목 기본 정보를 가져오지 못했습니다(잠시 후 재시도): code=" + code);
            }
            return new StockDetailDto(code, name, price, null, null, null);
        }
    }

    private String text(JsonNode root, String field) {
        JsonNode n = root.path(field);
        return n.isMissingNode() ? null : n.asText(null);
    }

    private String fromTotalInfos(JsonNode root, String code) {
        JsonNode arr = root.path("totalInfos");
        if (arr != null && arr.isArray()) {
            for (JsonNode it : arr) {
                if (code.equals(it.path("code").asText())) {
                    return it.path("value").asText(null);
                }
            }
        }
        return null;
    }

    private String fromDealTrendClose(JsonNode root) {
        JsonNode deal = root.path("dealTrendInfos");
        if (deal != null && deal.isArray() && deal.size() > 0) {
            return deal.get(0).path("closePrice").asText(null);
        }
        return null;
    }
}


