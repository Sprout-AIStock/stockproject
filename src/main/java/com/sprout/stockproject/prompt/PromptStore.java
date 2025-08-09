// src/main/java/com/sprout/stockproject/prompt/PromptStore.java
package com.sprout.stockproject.prompt;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PromptStore {
    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();
    public String get(String name) {
        return cache.computeIfAbsent(name, k -> {
            try (var in = new ClassPathResource("prompts/" + k + ".prompt").getInputStream()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            } catch (Exception e) {
                throw new RuntimeException("Prompt load failed: " + k, e);
            }
        });
    }
}