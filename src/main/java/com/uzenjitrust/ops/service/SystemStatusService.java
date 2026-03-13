package com.uzenjitrust.ops.service;

import com.uzenjitrust.common.config.DocsProperties;
import com.uzenjitrust.common.error.ForbiddenException;
import com.uzenjitrust.common.monitoring.JobExecutionRegistry;
import com.uzenjitrust.common.security.ActorPrincipal;
import com.uzenjitrust.common.security.ActorProvider;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.ops.domain.OutboxStatus;
import com.uzenjitrust.ops.repo.OutboxEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SystemStatusService {

    private final HealthEndpoint healthEndpoint;
    private final OutboxEventRepository outboxEventRepository;
    private final JobExecutionRegistry jobExecutionRegistry;
    private final ActorProvider actorProvider;
    private final DocsProperties docsProperties;
    private final boolean schedulingEnabled;

    public SystemStatusService(HealthEndpoint healthEndpoint,
                               OutboxEventRepository outboxEventRepository,
                               JobExecutionRegistry jobExecutionRegistry,
                               ActorProvider actorProvider,
                               DocsProperties docsProperties,
                               @Value("${spring.task.scheduling.enabled:true}") boolean schedulingEnabled) {
        this.healthEndpoint = healthEndpoint;
        this.outboxEventRepository = outboxEventRepository;
        this.jobExecutionRegistry = jobExecutionRegistry;
        this.actorProvider = actorProvider;
        this.docsProperties = docsProperties;
        this.schedulingEnabled = schedulingEnabled;
    }

    @Transactional(readOnly = true)
    public SystemHealthView currentStatus() {
        requireAdmin();

        HealthComponent root = healthEndpoint.health();
        HealthComponent db = healthEndpoint.healthForPath("db");

        Map<String, JobView> jobs = new LinkedHashMap<>();
        jobs.put("offer-expiry", toJobView("offer-expiry", "fixedDelay: ${app.market.offer-expiry-delay-ms:60000}"));
        jobs.put("reservation-expiry", toJobView("reservation-expiry", "fixedDelay: ${app.market.reservation-expiry-delay-ms:60000}"));
        jobs.put("retention-release", toJobView("retention-release", "cron: 0 0 * * * *"));
        jobs.put("outbox-dispatcher", toJobView("outbox-dispatcher", "fixedDelay: ${app.ops.outbox-dispatch-delay-ms:5000}"));

        return new SystemHealthView(
                status(root),
                status(db),
                schedulingEnabled,
                docsProperties.isEnabled(),
                new QueueView(
                        outboxEventRepository.countByStatus(OutboxStatus.PENDING),
                        outboxEventRepository.countByStatus(OutboxStatus.FAILED)
                ),
                jobs
        );
    }

    private JobView toJobView(String jobName, String schedule) {
        JobExecutionRegistry.JobStatusView snapshot = jobExecutionRegistry.snapshot(jobName, schedule).toView();
        return new JobView(
                snapshot.jobName(),
                schedulingEnabled,
                snapshot.schedule(),
                snapshot.lastStartedAt(),
                snapshot.lastCompletedAt(),
                snapshot.lastSuccessAt(),
                snapshot.lastError(),
                snapshot.lastMessage()
        );
    }

    private void requireAdmin() {
        ActorPrincipal actor = actorProvider.requireActor();
        if (!actor.roles().contains(AppRole.ADMIN)) {
            throw new ForbiddenException("Insufficient role");
        }
    }

    private String status(HealthComponent component) {
        if (component == null) {
            return Status.UNKNOWN.getCode();
        }
        return component.getStatus().getCode();
    }

    public record SystemHealthView(
            String overallStatus,
            String databaseStatus,
            boolean schedulingEnabled,
            boolean docsEnabled,
            QueueView queue,
            Map<String, JobView> jobs
    ) { }

    public record QueueView(
            long pendingOutboxEvents,
            long failedOutboxEvents
    ) { }

    public record JobView(
            String name,
            boolean enabled,
            String schedule,
            java.time.Instant lastStartedAt,
            java.time.Instant lastCompletedAt,
            java.time.Instant lastSuccessAt,
            String lastError,
            String lastMessage
    ) { }
}
