package com.sprout.stockproject.cache;

import com.sprout.stockproject.dto.MacroQuadInput;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class MacroSnapshotCache {

    private final AtomicReference<MacroQuadInput> latest = new AtomicReference<>();
    private final AtomicReference<Instant> updatedAt = new AtomicReference<>();

    public void put(MacroQuadInput input) {
        latest.set(input);
        updatedAt.set(Instant.now());
    }

    public Optional<MacroQuadInput> latest() {
        return Optional.ofNullable(latest.get());
    }

    public Instant lastUpdatedAt() {
        return updatedAt.get();
    }
}


