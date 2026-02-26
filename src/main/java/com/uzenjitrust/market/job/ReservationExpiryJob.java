package com.uzenjitrust.market.job;

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

    private final PropertyReservationRepository reservationRepository;

    public ReservationExpiryJob(PropertyReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @Scheduled(fixedDelayString = "${app.market.reservation-expiry-delay-ms:60000}")
    @Transactional
    public void expireReservations() {
        int expired = reservationRepository.expireReservations(Instant.now());
        if (expired > 0) {
            log.info("Expired {} reservations", expired);
        }
    }
}
