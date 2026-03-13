package com.uzenjitrust.market.job;

import com.uzenjitrust.common.monitoring.JobExecutionRegistry;
import com.uzenjitrust.market.repo.OfferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@Profile("!test")
public class OfferExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(OfferExpiryJob.class);
    private static final String JOB_NAME = "offer-expiry";
    private static final String SCHEDULE = "fixedDelay: ${app.market.offer-expiry-delay-ms:60000}";

    private final OfferRepository offerRepository;
    private final JobExecutionRegistry jobExecutionRegistry;

    public OfferExpiryJob(OfferRepository offerRepository,
                          JobExecutionRegistry jobExecutionRegistry) {
        this.offerRepository = offerRepository;
        this.jobExecutionRegistry = jobExecutionRegistry;
    }

    @Scheduled(fixedDelayString = "${app.market.offer-expiry-delay-ms:60000}")
    @Transactional
    public void expireOffers() {
        try {
            int expired = offerRepository.expireOffers(Instant.now());
            if (expired > 0) {
                log.info("Expired {} offers", expired);
            }
            jobExecutionRegistry.recordSuccess(JOB_NAME, SCHEDULE, "expired=" + expired);
        } catch (Exception ex) {
            jobExecutionRegistry.recordFailure(JOB_NAME, SCHEDULE, ex);
            throw ex;
        }
    }
}
