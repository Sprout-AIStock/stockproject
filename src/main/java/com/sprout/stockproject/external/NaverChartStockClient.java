package com.sprout.stockproject.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;

@Component
public class NaverChartStockClient {

    private final WebClient wc;
    private final WebClient wcJson;
    private final ObjectMapper om;

    public NaverChartStockClient(WebClient.Builder builder, ObjectMapper om) {
        this.wc = builder.baseUrl("https://fchart.stock.naver.com").build();
        this.wcJson = builder.clone().baseUrl("https://api.finance.naver.com").build();
        this.om = om;
    }

    /** 일봉 차트 데이터 조회 */
    public JsonNode fetchDailyChart(String stockCode, int count) {
        return fetchChartData(stockCode, "day", count);
    }

    /** 주봉 차트 데이터 조회 */
    public JsonNode fetchWeeklyChart(String stockCode, int count) {
        return fetchChartData(stockCode, "week", count);
    }

    /** 월봉 차트 데이터 조회 */
    public JsonNode fetchMonthlyChart(String stockCode, int count) {
        return fetchChartData(stockCode, "month", count);
    }

    /** 차트 데이터 조회 공통 메서드 */
    private JsonNode fetchChartData(String stockCode, String timeframe, int count) {
        try {
            String xml = wc.get()
                    .uri(uri -> uri.path("/sise.nhn")
                            .queryParam("symbol", stockCode)
                            .queryParam("timeframe", timeframe)
                            .queryParam("count", count)
                            .queryParam("requestType", "0")
                            .build())
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
                    .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("Referer", "https://m.stock.naver.com")
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            try {
                return parseXmlToJson(xml);
            } catch (Exception ignore) {
                // Fallback to JSON endpoint
                String json = wcJson.get()
                        .uri(u -> u.path("/siseJson.naver")
                                .queryParam("symbol", stockCode)
                                .queryParam("requestType", "1")
                                .queryParam("count", count)
                                .queryParam("timeframe", timeframe)
                                .build())
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
                        .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                        .retrieve()
                        .bodyToMono(String.class)
                        .block();
                return parseFallbackJson(json, stockCode, timeframe);
            }
        } catch (Exception e) {
            throw new RuntimeException("NaverChartStockClient.fetchChartData failed: " + e.getMessage(), e);
        }
    }

    /** XML을 JSON으로 변환 */
    private JsonNode parseXmlToJson(String xml) {
        try {
            String cleaned = sanitizeXml(xml);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(cleaned.getBytes("EUC-KR")));

            Element chartData = (Element) doc.getElementsByTagName("chartdata").item(0);
            String symbol = chartData.getAttribute("symbol");
            String name = chartData.getAttribute("name");
            String timeframe = chartData.getAttribute("timeframe");

            ObjectNode result = om.createObjectNode();
            result.put("symbol", symbol);
            result.put("name", name);
            result.put("timeframe", timeframe);

            ArrayNode dataArray = om.createArrayNode();
            NodeList items = chartData.getElementsByTagName("item");

            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                String data = item.getAttribute("data");
                String[] parts = data.split("\\|");

                if (parts.length >= 6) {
                    ObjectNode dataNode = om.createObjectNode();
                    dataNode.put("date", parts[0]);
                    dataNode.put("open", parts[1]);
                    dataNode.put("high", parts[2]);
                    dataNode.put("low", parts[3]);
                    dataNode.put("close", parts[4]);
                    dataNode.put("volume", parts[5]);
                    dataArray.add(dataNode);
                }
            }

            result.set("data", dataArray);
            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse XML: " + e.getMessage(), e);
        }
    }

    private String sanitizeXml(String raw) {
        if (raw == null) return "";
        String s = raw.replace("\uFEFF", ""); // BOM 제거
        int idx = s.indexOf('<');
        if (idx > 0) {
            s = s.substring(idx);
        }
        return s.trim();
    }

    private JsonNode parseFallbackJson(String json, String stockCode, String timeframe) throws Exception {
        if (json == null || json.isBlank()) throw new IllegalArgumentException("Empty JSON");
        JsonNode root = om.readTree(json);
        if (!root.isArray() || root.size() < 2) throw new IllegalArgumentException("Unexpected JSON chart format");
        // root[0] is header row ["날짜","시가","고가","저가","종가","거래량"]
        ArrayNode dataArray = om.createArrayNode();
        for (int i = 1; i < root.size(); i++) {
            JsonNode row = root.get(i);
            if (row.isArray() && row.size() >= 6) {
                ObjectNode n = om.createObjectNode();
                n.put("date", row.get(0).asText());
                n.put("open", row.get(1).asText());
                n.put("high", row.get(2).asText());
                n.put("low", row.get(3).asText());
                n.put("close", row.get(4).asText());
                n.put("volume", row.get(5).asText());
                dataArray.add(n);
            }
        }
        ObjectNode result = om.createObjectNode();
        result.put("symbol", stockCode);
        result.put("name", stockCode);
        result.put("timeframe", timeframe);
        result.set("data", dataArray);
        return result;
    }
}
