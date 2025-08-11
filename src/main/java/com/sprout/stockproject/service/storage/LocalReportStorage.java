package com.sprout.stockproject.service.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.stream.Stream;

@Component
public class LocalReportStorage implements ReportStorage {

    private final Path root;

    public LocalReportStorage(@Value("${report.storage.root:data/reports}") String rootDir) {
        this.root = Path.of(rootDir);
    }

    @Override
    public String saveToday(String markdown) {
        try {
            Files.createDirectories(root);
            String name = LocalDate.now().toString().replace("-", "");
            Path p = root.resolve(name + ".md");
            Files.writeString(p, markdown, StandardCharsets.UTF_8);
            return p.toString();
        } catch (IOException e) {
            throw new RuntimeException("Report save failed", e);
        }
    }

    @Override
    public String loadLatest() {
        try {
            if (!Files.exists(root)) return null;
            try (Stream<Path> s = Files.list(root)) {
                Path latest = s.filter(p -> p.getFileName().toString().endsWith(".md"))
                        .max(Comparator.comparing(Path::getFileName))
                        .orElse(null);
                if (latest == null) return null;
                return Files.readString(latest, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("Report loadLatest failed", e);
        }
    }

    @Override
    public String loadByDate(String yyyymmdd) {
        try {
            Path p = root.resolve(yyyymmdd + ".md");
            if (!Files.exists(p)) return null;
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Report loadByDate failed", e);
        }
    }
}


