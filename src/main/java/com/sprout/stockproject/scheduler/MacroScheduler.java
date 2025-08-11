package com.sprout.stockproject.scheduler;

import com.sprout.stockproject.cache.MacroSnapshotCache;
import com.sprout.stockproject.dto.MacroQuadInput;
import com.sprout.stockproject.service.MacroQuadIngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MacroScheduler {
    private static final Logger log = LoggerFactory.getLogger(MacroScheduler.class);

    private final MacroQuadIngestService ingest;
    private final MacroSnapshotCache cache;

    public MacroScheduler(MacroQuadIngestService ingest, MacroSnapshotCache cache) {
        this.ingest = ingest;
        this.cache = cache;
    }

    // 매일 08:05 KST
    @Scheduled(cron = "0 5 8 * * *", zone = "Asia/Seoul")
    public void dailyMorning() {
        runIngest("dailyMorning");
    }

    // 매주 목요일 21:40 KST (주간 청구건)
    @Scheduled(cron = "0 40 21 * * THU", zone = "Asia/Seoul")
    public void weeklyClaims() {
        runIngest("weeklyClaims");
    }

    // 매월 첫째 금요일 21:40 KST (고용지표)
    @Scheduled(cron = "0 40 21 1-7 * FRI", zone = "Asia/Seoul")
    public void monthlyPayroll() {
        runIngest("monthlyPayroll");
    }

    private void runIngest(String tag) {
        try {
            MacroQuadInput input = ingest.buildLatestInput();
            cache.put(input);
            log.info("[MacroScheduler][{}] cache updated: asOf={}", tag, input.asOf());
        } catch (Exception e) {
            log.error("[MacroScheduler][{}] ingest failed: {}", tag, e.getMessage(), e);
        }
    }
}


