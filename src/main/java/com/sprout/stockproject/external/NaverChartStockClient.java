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
    private final ObjectMapper om;

    public NaverChartStockClient(WebClient.Builder builder, ObjectMapper om) {
        this.wc = builder.baseUrl("https://fchart.stock.naver.com").build();
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
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseXmlToJson(xml);
        } catch (Exception e) {
            throw new RuntimeException("NaverChartStockClient.fetchChartData failed: " + e.getMessage(), e);
        }
    }

    /** XML을 JSON으로 변환 */
    private JsonNode parseXmlToJson(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes("EUC-KR")));

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
}
