package com.uzenjitrust.common.monitoring;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JobExecutionRegistry {

    private final Map<String, JobExecutionSnapshot> snapshots = new ConcurrentHashMap<>();

    public void recordSuccess(String jobName, String schedule, String details) {
        snapshots.compute(jobName, (name, existing) -> {
            Instant now = Instant.now();
            JobExecutionSnapshot snapshot = existing == null
                    ? new JobExecutionSnapshot(jobName, schedule)
                    : existing;
            snapshot.setLastStartedAt(now);
            snapshot.setLastCompletedAt(now);
            snapshot.setLastSuccessAt(now);
            snapshot.setLastError(null);
            snapshot.setLastMessage(details);
            return snapshot;
        });
    }

    public void recordFailure(String jobName, String schedule, Exception error) {
        snapshots.compute(jobName, (name, existing) -> {
            Instant now = Instant.now();
            JobExecutionSnapshot snapshot = existing == null
                    ? new JobExecutionSnapshot(jobName, schedule)
                    : existing;
            snapshot.setLastStartedAt(now);
            snapshot.setLastCompletedAt(now);
            snapshot.setLastError(error == null ? null : error.getMessage());
            return snapshot;
        });
    }

    public JobExecutionSnapshot snapshot(String jobName, String schedule) {
        return snapshots.computeIfAbsent(jobName, name -> new JobExecutionSnapshot(jobName, schedule));
    }

    public record JobStatusView(
            String jobName,
            String schedule,
            Instant lastStartedAt,
            Instant lastCompletedAt,
            Instant lastSuccessAt,
            String lastError,
            String lastMessage
    ) {
    }

    public static final class JobExecutionSnapshot {
        private final String jobName;
        private final String schedule;
        private Instant lastStartedAt;
        private Instant lastCompletedAt;
        private Instant lastSuccessAt;
        private String lastError;
        private String lastMessage;

        private JobExecutionSnapshot(String jobName, String schedule) {
            this.jobName = jobName;
            this.schedule = schedule;
        }

        public JobStatusView toView() {
            return new JobStatusView(jobName, schedule, lastStartedAt, lastCompletedAt, lastSuccessAt, lastError, lastMessage);
        }

        public void setLastStartedAt(Instant lastStartedAt) {
            this.lastStartedAt = lastStartedAt;
        }

        public void setLastCompletedAt(Instant lastCompletedAt) {
            this.lastCompletedAt = lastCompletedAt;
        }

        public void setLastSuccessAt(Instant lastSuccessAt) {
            this.lastSuccessAt = lastSuccessAt;
        }

        public void setLastError(String lastError) {
            this.lastError = lastError;
        }

        public void setLastMessage(String lastMessage) {
            this.lastMessage = lastMessage;
        }
    }
}
