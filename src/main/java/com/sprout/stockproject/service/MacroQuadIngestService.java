package com.sprout.stockproject.service;

import com.sprout.stockproject.dto.MacroQuadInput;
import com.sprout.stockproject.external.fred.FredClient;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class MacroQuadIngestService {

    private final FredClient fred;

    public MacroQuadIngestService(FredClient fred) {
        this.fred = fred;
    }

    /**
     * Pulls indicators from FRED and assembles MacroQuadInput.
     * Allows nulls when some series are missing.
     */
    public MacroQuadInput buildLatestInput() {
        // Series IDs
        String DFEDTARU = "DFEDTARU";  // Federal Funds Target Range - Upper Limit (Percent)
        String PCEPILFE = "PCEPILFE";  // Personal Consumption Expenditures Excluding Food and Energy (Index)
        String CPILFESL = "CPILFESL";  // Core CPI Index
        String UNRATE   = "UNRATE";    // Unemployment Rate
        String PAYEMS   = "PAYEMS";    // Total Nonfarm Payrolls
        String IC4WSA   = "IC4WSA";    // 4-Week Moving Average of Initial Claims

        Double ffrUpper = fred.latestValue(DFEDTARU);
        Double corePceYoy = fred.yoyFromIndex(PCEPILFE);
        Double coreCpiYoy = corePceYoy == null ? fred.yoyFromIndex(CPILFESL) : null; // prefer PCE; fallback CPI

        Double unemp = fred.latestValue(UNRATE);
        Double unemp3mAgo = valueMonthsAgo(UNRATE, 3);
        Double unempChange3m = (unemp != null && unemp3mAgo != null) ? round1(unemp - unemp3mAgo) : null;

        Double payrolls3mma = threeMonthAverage(PAYEMS);

        Double claims4wma = fred.latestValue(IC4WSA);
        String claimsTrend = claimsTrend(IC4WSA);

        Integer policyRateChange3mBps = policyRateChange3mBps(DFEDTARU);

        return new MacroQuadInput(
                OffsetDateTime.now().toString(),
                null, // gdpNow_qoq_saar_pct (외부 소스 미도입)
                payrolls3mma == null ? null : payrolls3mma,
                unemp,
                unempChange3m,
                claims4wma,
                claimsTrend,
                null, // pmi_mfg
                null, // pmi_svcs
                ffrUpper,
                corePceYoy,
                coreCpiYoy,
                policyRateChange3mBps,
                null // previousDecision
        );
    }

    private Double valueMonthsAgo(String seriesId, int months) {
        List<FredClient.FredPoint> asc = fred.lastN(seriesId, 15);
        if (asc.isEmpty()) return null;
        // naive: last element as latest; approximate months-ago by index offset
        int idx = asc.size() - 1 - months;
        if (idx < 0) return null;
        return asc.get(idx).value();
    }

    private Double threeMonthAverage(String seriesId) {
        List<FredClient.FredPoint> asc = fred.lastN(seriesId, 3);
        if (asc.size() < 3) return null;
        double sum = 0;
        for (FredClient.FredPoint p : asc) {
            if (p.value() == null) return null;
            sum += p.value();
        }
        return round0(sum / 3.0);
    }

    private String claimsTrend(String seriesId) {
        List<FredClient.FredPoint> asc = fred.lastN(seriesId, 8); // about 8 weeks
        if (asc.size() < 8) return null;
        double recent4 = avgTail(asc, 4);
        double prev4 = avgWindowFromTail(asc, 8, 4);
        double diff = recent4 - prev4;
        if (Math.abs(diff) < 1.0) return "flat"; // threshold 1k
        return diff > 0 ? "up" : "down";
    }

    private Integer policyRateChange3mBps(String seriesId) {
        List<FredClient.FredPoint> asc = fred.lastN(seriesId, 4);
        if (asc.size() < 4) return null;
        Double latest = asc.get(asc.size() - 1).value();
        Double prev3 = asc.get(asc.size() - 4).value();
        if (latest == null || prev3 == null) return null;
        double delta = (latest - prev3) * 100.0;
        return (int) Math.round(delta);
    }

    private double avgTail(List<FredClient.FredPoint> asc, int count) {
        double sum = 0; int n = 0;
        for (int i = Math.max(0, asc.size() - count); i < asc.size(); i++) {
            Double v = asc.get(i).value();
            if (v == null) return Double.NaN;
            sum += v; n++;
        }
        return sum / n;
    }

    private double avgWindowFromTail(List<FredClient.FredPoint> asc, int windowTail, int windowSize) {
        int end = Math.max(0, asc.size() - windowTail);
        int start = Math.max(0, end - windowSize);
        double sum = 0; int n = 0;
        for (int i = start; i < end; i++) {
            Double v = asc.get(i).value();
            if (v == null) return Double.NaN;
            sum += v; n++;
        }
        return n == 0 ? Double.NaN : sum / n;
    }

    private Double round0(double v) { return (double) Math.round(v); }
    private Double round1(double v) { return Math.round(v * 10.0) / 10.0; }
}


