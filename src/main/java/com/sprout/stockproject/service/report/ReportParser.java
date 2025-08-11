package com.sprout.stockproject.service.report;

import java.util.ArrayList;
import java.util.List;

public class ReportParser {

    public record Section(String title, String content, int startLine, int endLine) {}

    public List<Section> parseSections(String markdown) {
        String[] lines = markdown.split("\n");
        List<Section> sections = new ArrayList<>();
        String currentTitle = "(본문)";
        StringBuilder buf = new StringBuilder();
        int start = 1;
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("# ") || line.startsWith("## ")) {
                if (buf.length() > 0) {
                    sections.add(new Section(currentTitle, buf.toString().trim(), start, i));
                }
                currentTitle = line.replaceFirst("^#+ ", "").trim();
                buf.setLength(0);
                start = i + 1;
            } else {
                buf.append(line).append('\n');
            }
        }
        if (buf.length() > 0) {
            sections.add(new Section(currentTitle, buf.toString().trim(), start, lines.length));
        }
        return sections;
    }
}


