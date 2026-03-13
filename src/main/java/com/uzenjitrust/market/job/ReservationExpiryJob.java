package com.uzenjitrust.market.job;

import com.uzenjitrust.common.monitoring.JobExecutionRegistry;
import com.uzenjitrust.market.repo.PropertyReservationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@Profile("!test")
public class ReservationExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(ReservationExpiryJob.class);
    private static final String JOB_NAME = "reservation-expiry";
    private static final String SCHEDULE = "fixedDelay: ${app.market.reservation-expiry-delay-ms:60000}";

    private final PropertyReservationRepository reservationRepository;
    private final JobExecutionRegistry jobExecutionRegistry;

    public ReservationExpiryJob(PropertyReservationRepository reservationRepository,
                                JobExecutionRegistry jobExecutionRegistry) {
        this.reservationRepository = reservationRepository;
        this.jobExecutionRegistry = jobExecutionRegistry;
    }

    @Scheduled(fixedDelayString = "${app.market.reservation-expiry-delay-ms:60000}")
    @Transactional
    public void expireReservations() {
        try {
            int expired = reservationRepository.expireReservations(Instant.now());
            if (expired > 0) {
                log.info("Expired {} reservations", expired);
            }
            jobExecutionRegistry.recordSuccess(JOB_NAME, SCHEDULE, "expired=" + expired);
        } catch (Exception ex) {
            jobExecutionRegistry.recordFailure(JOB_NAME, SCHEDULE, ex);
            throw ex;
        }
    }
}
