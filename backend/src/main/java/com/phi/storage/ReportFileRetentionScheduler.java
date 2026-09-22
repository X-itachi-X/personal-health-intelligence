package com.phi.storage;

import com.phi.config.PhiProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ReportFileRetentionScheduler {

    private final PhiProperties properties;
    private final ReportFileStorageService storageService;

    public ReportFileRetentionScheduler(PhiProperties properties, ReportFileStorageService storageService) {
        this.properties = properties;
        this.storageService = storageService;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void purgeExpiredFiles() {
        if (properties.storage().effectiveRetainFilesDays() <= 0) {
            return;
        }
        storageService.purgeExpiredFiles();
    }
}
