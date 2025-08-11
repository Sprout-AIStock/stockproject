package com.sprout.stockproject.external.fred;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * FRED API client for time series observations.
 * - Provides utilities to fetch latest value, last N observations, and YoY from index-type series.
 */
@Component
public class FredClient {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${fred.api.key:}")
    private String apiKey;

    public FredClient(WebClient.Builder base, ObjectMapper objectMapper) {
        // Clone base builder and set FRED base URL
        this.webClient = base.clone().baseUrl("https://api.stlouisfed.org").build();
        this.objectMapper = objectMapper;
    }

    /** Observation point. */
    public record FredPoint(LocalDate date, Double value) {}

    /** Returns the most recent non-null numeric value for a series, or null if none. */
    public Double latestValue(String seriesId) {
        List<FredPoint> points = fetch(seriesId, 10); // fetch a handful and pick the first numeric
        for (FredPoint p : points) {
            if (p.value() != null) return p.value();
        }
        return null;
    }

    /** Returns last N observations (ascending by date). May return fewer if not available. */
    public List<FredPoint> lastN(String seriesId, int n) {
        List<FredPoint> desc = fetch(seriesId, n);
        // API returns newest-first; reverse to ascending for easier calculations
        List<FredPoint> asc = new ArrayList<>(desc);
        Collections.reverse(asc);
        return asc;
    }

    /**
     * Computes YoY percentage from index-type series (e.g., PCEPILFE, CPILFESL).
     * Returns value rounded to 1 decimal, or null if insufficient data.
     */
    public Double yoyFromIndex(String seriesId) {
        List<FredPoint> asc = lastN(seriesId, 14); // up to 14 to handle missing months
        if (asc.size() < 13) return null;
        FredPoint latest = asc.get(asc.size() - 1);
        // Find a point that is approximately 12 months prior by date
        LocalDate target = latest.date().minusMonths(12);
        FredPoint prior = findClosestOnOrBefore(asc, target);
        if (latest.value() == null || prior == null || prior.value() == null) return null;
        double yoy = (latest.value() / prior.value() - 1.0) * 100.0;
        return BigDecimal.valueOf(yoy).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private FredPoint findClosestOnOrBefore(List<FredPoint> asc, LocalDate target) {
        FredPoint candidate = null;
        for (FredPoint p : asc) {
            if (!p.date().isAfter(target)) {
                if (candidate == null || p.date().isAfter(candidate.date())) candidate = p;
            } else {
                break;
            }
        }
        return candidate;
    }

    /** Fetches newest-first observations and parses into FredPoint list. */
    private List<FredPoint> fetch(String seriesId, int limit) {
        ensureApiKey();
        try {
            String raw = webClient.get()
                    .uri(uri -> uri.path("/fred/series/observations")
                            .queryParam("series_id", seriesId)
                            .queryParam("api_key", apiKey)
                            .queryParam("file_type", "json")
                            .queryParam("sort_order", "desc")
                            .queryParam("limit", String.valueOf(Math.max(1, limit)))
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (raw == null || raw.isBlank()) return List.of();
            JsonNode root = objectMapper.readTree(raw);
            JsonNode observations = root.path("observations");
            if (!observations.isArray()) return List.of();

            List<FredPoint> out = new ArrayList<>();
            for (JsonNode it : observations) {
                LocalDate date = LocalDate.parse(it.path("date").asText());
                String v = it.path("value").asText();
                Double val = parseValue(v);
                out.add(new FredPoint(date, val));
            }
            // Already newest-first by sort_order. Keep as is for latestValue; reverse in lastN.
            return out;
        } catch (Exception e) {
            throw new RuntimeException("FredClient.fetch failed: " + e.getMessage(), e);
        }
    }

    private Double parseValue(String valueText) {
        if (valueText == null) return null;
        String v = valueText.trim();
        if (v.isEmpty() || Objects.equals(v, ".")) return null;
        try {
            return Double.parseDouble(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private void ensureApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("FRED API key missing: set 'fred.api.key'");
        }
    }
}


