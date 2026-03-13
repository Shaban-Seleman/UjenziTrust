package com.uzenjitrust.integration;

import com.uzenjitrust.common.error.AppException;
import com.uzenjitrust.common.error.ConflictException;
import com.uzenjitrust.common.error.ForbiddenException;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.market.api.SubmitOfferRequest;
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
import com.uzenjitrust.market.service.PropertyService;
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
    @Autowired
    private PropertyService propertyService;

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

    @Test
    void getPropertyAllowsPublishedReadButKeepsDraftOwnerOnly() {
        UUID ownerId = TestSecurity.randomUser();
        UUID buyerId = TestSecurity.randomUser();

        PropertyEntity published = new PropertyEntity();
        published.setOwnerUserId(ownerId);
        published.setTitle("Mikocheni house");
        published.setDescription("Published listing");
        published.setLocation("Dar es Salaam");
        published.setAskingPrice(new BigDecimal("350000000"));
        published.setCurrency("TZS");
        published.setStatus(PropertyStatus.PUBLISHED);
        published = propertyRepository.save(published);

        PropertyEntity draft = new PropertyEntity();
        draft.setOwnerUserId(ownerId);
        draft.setTitle("Masaki penthouse");
        draft.setDescription("Draft listing");
        draft.setLocation("Dar es Salaam");
        draft.setAskingPrice(new BigDecimal("900000000"));
        draft.setCurrency("TZS");
        draft.setStatus(PropertyStatus.DRAFT);
        draft = propertyRepository.save(draft);
        UUID draftId = draft.getId();

        TestSecurity.as(buyerId, AppRole.BUYER);
        assertEquals(published.getId(), propertyService.getById(published.getId()).getId());
        assertThrows(ForbiddenException.class, () -> propertyService.getById(draftId));

        TestSecurity.as(ownerId, AppRole.OWNER);
        assertEquals(draftId, propertyService.getById(draftId).getId());
    }

    @Test
    void listMineReturnsOwnerPropertiesIncludingDraftsOnly() {
        UUID ownerId = TestSecurity.randomUser();
        UUID otherOwnerId = TestSecurity.randomUser();

        PropertyEntity ownerDraft = new PropertyEntity();
        ownerDraft.setOwnerUserId(ownerId);
        ownerDraft.setTitle("Owner draft");
        ownerDraft.setLocation("Dar es Salaam");
        ownerDraft.setAskingPrice(new BigDecimal("120000000"));
        ownerDraft.setCurrency("TZS");
        ownerDraft.setStatus(PropertyStatus.DRAFT);
        propertyRepository.save(ownerDraft);

        PropertyEntity ownerPublished = new PropertyEntity();
        ownerPublished.setOwnerUserId(ownerId);
        ownerPublished.setTitle("Owner published");
        ownerPublished.setLocation("Arusha");
        ownerPublished.setAskingPrice(new BigDecimal("220000000"));
        ownerPublished.setCurrency("TZS");
        ownerPublished.setStatus(PropertyStatus.PUBLISHED);
        propertyRepository.save(ownerPublished);

        PropertyEntity otherDraft = new PropertyEntity();
        otherDraft.setOwnerUserId(otherOwnerId);
        otherDraft.setTitle("Other owner draft");
        otherDraft.setLocation("Mwanza");
        otherDraft.setAskingPrice(new BigDecimal("180000000"));
        otherDraft.setCurrency("TZS");
        otherDraft.setStatus(PropertyStatus.DRAFT);
        propertyRepository.save(otherDraft);

        TestSecurity.as(ownerId, AppRole.OWNER);

        var results = propertyService.listMine(0, 20, "createdAt", org.springframework.data.domain.Sort.Direction.DESC);

        assertEquals(2, results.getTotalElements());
        assertEquals(2, results.getContent().size());
        assertEquals(ownerId, results.getContent().get(0).getOwnerUserId());
        assertEquals(ownerId, results.getContent().get(1).getOwnerUserId());
    }

    @Test
    void submitOfferPreventsDuplicateOpenOfferBySameBuyerForSameProperty() {
        UUID sellerId = TestSecurity.randomUser();
        UUID buyerId = TestSecurity.randomUser();

        PropertyEntity property = new PropertyEntity();
        property.setOwnerUserId(sellerId);
        property.setTitle("Upanga apartment");
        property.setDescription("Investor unit");
        property.setLocation("Dar es Salaam");
        property.setAskingPrice(new BigDecimal("257500000"));
        property.setCurrency("TZS");
        property.setStatus(PropertyStatus.PUBLISHED);
        property = propertyRepository.save(property);

        TestSecurity.as(buyerId, AppRole.BUYER);
        OfferEntity first = offerService.submit(
                property.getId(),
                new SubmitOfferRequest(new BigDecimal("250000000"), "TZS", "first offer")
        );

        ConflictException duplicate = assertThrows(ConflictException.class, () -> offerService.submit(
                property.getId(),
                new SubmitOfferRequest(new BigDecimal("890000000000000"), "TZS", "duplicate open offer")
        ));

        assertEquals("Buyer already has an open offer for this property", duplicate.getMessage());
        assertEquals(1, offerRepository.findByProperty_IdAndStatusIn(property.getId(), List.of(OfferStatus.SUBMITTED, OfferStatus.COUNTERED)).size());
        assertEquals(first.getId(), offerRepository.findByProperty_IdAndStatusIn(property.getId(), List.of(OfferStatus.SUBMITTED, OfferStatus.COUNTERED)).getFirst().getId());
    }

    @Test
    void submitOfferAllowsNewOfferAfterPreviousOfferIsWithdrawn() {
        UUID sellerId = TestSecurity.randomUser();
        UUID buyerId = TestSecurity.randomUser();

        PropertyEntity property = new PropertyEntity();
        property.setOwnerUserId(sellerId);
        property.setTitle("Mbezi beach house");
        property.setDescription("Ocean view");
        property.setLocation("Dar es Salaam");
        property.setAskingPrice(new BigDecimal("310000000"));
        property.setCurrency("TZS");
        property.setStatus(PropertyStatus.PUBLISHED);
        property = propertyRepository.save(property);

        TestSecurity.as(buyerId, AppRole.BUYER);
        OfferEntity first = offerService.submit(
                property.getId(),
                new SubmitOfferRequest(new BigDecimal("300000000"), "TZS", "initial")
        );
        offerService.withdraw(first.getId(), "replacing bid");

        OfferEntity replacement = offerService.submit(
                property.getId(),
                new SubmitOfferRequest(new BigDecimal("320000000"), "TZS", "replacement")
        );

        assertNotNull(replacement.getId());
        assertEquals(OfferStatus.SUBMITTED, replacement.getStatus());
        assertEquals(1, offerRepository.findByProperty_IdAndStatusIn(property.getId(), List.of(OfferStatus.SUBMITTED, OfferStatus.COUNTERED)).size());
        assertEquals(new BigDecimal("320000000"), replacement.getAmount());
    }
}
