package com.uzenjitrust.build.job;

import com.uzenjitrust.common.monitoring.JobExecutionRegistry;
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
    private static final String JOB_NAME = "retention-release";
    private static final String SCHEDULE = "cron: 0 0 * * * *";

    private final RetentionOrchestrator retentionOrchestrator;
    private final JobExecutionRegistry jobExecutionRegistry;

    public RetentionReleaseJob(RetentionOrchestrator retentionOrchestrator,
                               JobExecutionRegistry jobExecutionRegistry) {
        this.retentionOrchestrator = retentionOrchestrator;
        this.jobExecutionRegistry = jobExecutionRegistry;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void runHourly() {
        try {
            int count = retentionOrchestrator.releaseDueRetentionsSystem();
            if (count > 0) {
                log.info("Released retention for {} milestone(s)", count);
            }
            jobExecutionRegistry.recordSuccess(JOB_NAME, SCHEDULE, "released=" + count);
        } catch (Exception ex) {
            jobExecutionRegistry.recordFailure(JOB_NAME, SCHEDULE, ex);
            throw ex;
        }
    }
}
