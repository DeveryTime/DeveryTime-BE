package com.dms.deverytime.domain.auth.scheduler;

import com.dms.deverytime.domain.auth.service.RefreshTokenCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {
    private final RefreshTokenCleanupService refreshTokenCleanupService;

    @Scheduled(cron = "${app.refresh-token-cleanup.cron:0 0 4 * * *}", zone = "Asia/Seoul")
    public void deleteExpiredTokens() {
        int deletedCount = refreshTokenCleanupService.deleteExpiredTokens();
        log.info("Expired refresh tokens deleted: {}", deletedCount);
    }
}
