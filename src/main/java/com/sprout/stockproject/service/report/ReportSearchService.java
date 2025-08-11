package com.sprout.stockproject.service.report;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ReportSearchService {

    public record Snippet(String title, String snippet, int startLine, int endLine, double score) {}

    private final ReportParser parser = new ReportParser();

    public List<Snippet> topK(String markdown, String query, int k) {
        var sections = parser.parseSections(markdown);
        List<Snippet> out = new ArrayList<>();
        for (var s : sections) {
            double score = scoreBm25Approx(s.content(), query);
            if (score > 0) {
                out.add(new Snippet(s.title(), firstLines(s.content(), 5), s.startLine(), s.endLine(), score));
            }
        }
        out.sort(Comparator.comparingDouble(Snippet::score).reversed());
        if (out.size() > k) return out.subList(0, k);
        return out;
    }

    private String firstLines(String content, int n) {
        String[] ls = content.split("\n");
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < Math.min(n, ls.length); i++) b.append(ls[i]).append('\n');
        return b.toString().trim();
    }

    // 매우 단순한 BM25 근사치(의존성 없이)
    private double scoreBm25Approx(String doc, String query) {
        String[] q = query.toLowerCase().split("\\s+");
        String lower = doc.toLowerCase();
        int hits = 0;
        for (String t : q) {
            if (t.isBlank()) continue;
            if (lower.contains(t)) hits++;
        }
        if (hits == 0) return 0;
        double lenPenalty = 1.0 / Math.log(10 + doc.length());
        return hits * lenPenalty;
    }
}


