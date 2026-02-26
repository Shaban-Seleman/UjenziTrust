package com.uzenjitrust.integration;

import com.uzenjitrust.common.error.AppException;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.market.api.AcceptOfferRequest;
import com.uzenjitrust.market.domain.OfferEntity;
import com.uzenjitrust.market.domain.OfferStatus;
import com.uzenjitrust.market.domain.PropertyEntity;
import com.uzenjitrust.market.domain.PropertyReservationEntity;
import com.uzenjitrust.market.domain.PropertyStatus;
import com.uzenjitrust.market.domain.ReservationStatus;
import com.uzenjitrust.market.repo.OfferRepository;
import com.uzenjitrust.market.repo.PropertyRepository;
import com.uzenjitrust.market.repo.PropertyReservationRepository;
import com.uzenjitrust.market.service.OfferService;
import com.uzenjitrust.support.PostgresIntegrationTest;
import com.uzenjitrust.support.TestSecurity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MarketplaceIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private PropertyRepository propertyRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private PropertyReservationRepository reservationRepository;
    @Autowired
    private OfferService offerService;

    @Test
    void acceptOfferPreventsDoubleReservation() {
        UUID sellerId = TestSecurity.randomUser();
        UUID buyerA = TestSecurity.randomUser();
        UUID buyerB = TestSecurity.randomUser();

        PropertyEntity property = new PropertyEntity();
        property.setOwnerUserId(sellerId);
        property.setTitle("Kigamboni plot");
        property.setDescription("Prime property");
        property.setLocation("Dar es Salaam");
        property.setAskingPrice(new BigDecimal("100000000"));
        property.setCurrency("TZS");
        property.setStatus(PropertyStatus.PUBLISHED);
        property = propertyRepository.save(property);

        OfferEntity offerA = new OfferEntity();
        offerA.setProperty(property);
        offerA.setBuyerUserId(buyerA);
        offerA.setSellerUserId(sellerId);
        offerA.setAmount(new BigDecimal("95000000"));
        offerA.setCurrency("TZS");
        offerA.setStatus(OfferStatus.SUBMITTED);
        offerA = offerRepository.save(offerA);

        OfferEntity offerB = new OfferEntity();
        offerB.setProperty(property);
        offerB.setBuyerUserId(buyerB);
        offerB.setSellerUserId(sellerId);
        offerB.setAmount(new BigDecimal("97000000"));
        offerB.setCurrency("TZS");
        offerB.setStatus(OfferStatus.SUBMITTED);
        offerB = offerRepository.save(offerB);
        final UUID offerBId = offerB.getId();

        TestSecurity.as(sellerId, AppRole.SELLER);
        OfferService.AcceptOfferResult accepted = offerService.accept(
                offerA.getId(),
                new AcceptOfferRequest("acc-1", "accept first")
        );

        assertEquals(OfferStatus.ACCEPTED, accepted.offer().getStatus());
        assertNotNull(accepted.reservation().getId());
        assertEquals(ReservationStatus.ACTIVE, accepted.reservation().getStatus());

        assertThrows(AppException.class, () -> offerService.accept(
                offerBId,
                new AcceptOfferRequest("acc-2", "try second")
        ));

        PropertyReservationEntity reservation = reservationRepository
                .findByProperty_IdAndStatus(property.getId(), ReservationStatus.ACTIVE)
                .orElseThrow();
        assertEquals(offerA.getId(), reservation.getOfferId());
    }
}
