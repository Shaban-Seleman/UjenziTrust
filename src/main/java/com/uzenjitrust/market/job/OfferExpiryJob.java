package com.uzenjitrust.market.job;

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

    private final OfferRepository offerRepository;

    public OfferExpiryJob(OfferRepository offerRepository) {
        this.offerRepository = offerRepository;
    }

    @Scheduled(fixedDelayString = "${app.market.offer-expiry-delay-ms:60000}")
    @Transactional
    public void expireOffers() {
        int expired = offerRepository.expireOffers(Instant.now());
        if (expired > 0) {
            log.info("Expired {} offers", expired);
        }
    }
}
