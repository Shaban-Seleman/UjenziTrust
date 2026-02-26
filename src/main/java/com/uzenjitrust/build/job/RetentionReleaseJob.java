package com.uzenjitrust.build.job;

import com.uzenjitrust.build.orchestrator.RetentionOrchestrator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
public class RetentionReleaseJob {

    private static final Logger log = LoggerFactory.getLogger(RetentionReleaseJob.class);

    private final RetentionOrchestrator retentionOrchestrator;

    public RetentionReleaseJob(RetentionOrchestrator retentionOrchestrator) {
        this.retentionOrchestrator = retentionOrchestrator;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void runHourly() {
        int count = retentionOrchestrator.releaseDueRetentionsSystem();
        if (count > 0) {
            log.info("Released retention for {} milestone(s)", count);
        }
    }
}
