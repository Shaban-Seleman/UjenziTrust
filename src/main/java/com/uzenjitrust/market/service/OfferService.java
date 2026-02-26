package com.uzenjitrust.market.service;

import com.uzenjitrust.common.error.BadRequestException;
import com.uzenjitrust.common.error.ConflictException;
import com.uzenjitrust.common.error.ForbiddenException;
import com.uzenjitrust.common.error.NotFoundException;
import com.uzenjitrust.common.idempotency.IdempotencyService;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.common.security.AuthorizationService;
import com.uzenjitrust.market.api.AcceptOfferRequest;
import com.uzenjitrust.market.api.CounterOfferRequest;
import com.uzenjitrust.market.api.SubmitOfferRequest;
import com.uzenjitrust.market.domain.OfferEntity;
import com.uzenjitrust.market.domain.OfferEventEntity;
import com.uzenjitrust.market.domain.OfferStatus;
import com.uzenjitrust.market.domain.PropertyEntity;
import com.uzenjitrust.market.domain.PropertyReservationEntity;
import com.uzenjitrust.market.domain.PropertyStatus;
import com.uzenjitrust.market.domain.ReservationStatus;
import com.uzenjitrust.market.orchestrator.PropertyPurchaseOrchestrator;
import com.uzenjitrust.market.repo.OfferEventRepository;
import com.uzenjitrust.market.repo.OfferRepository;
import com.uzenjitrust.market.repo.PropertyRepository;
import com.uzenjitrust.market.repo.PropertyReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class OfferService {

    private final OfferRepository offerRepository;
    private final OfferEventRepository offerEventRepository;
    private final PropertyRepository propertyRepository;
    private final PropertyReservationRepository reservationRepository;
    private final AuthorizationService authorizationService;
    private final PropertyPurchaseOrchestrator propertyPurchaseOrchestrator;
    private final IdempotencyService idempotencyService;

    public OfferService(OfferRepository offerRepository,
                        OfferEventRepository offerEventRepository,
                        PropertyRepository propertyRepository,
                        PropertyReservationRepository reservationRepository,
                        AuthorizationService authorizationService,
                        PropertyPurchaseOrchestrator propertyPurchaseOrchestrator,
                        IdempotencyService idempotencyService) {
        this.offerRepository = offerRepository;
        this.offerEventRepository = offerEventRepository;
        this.propertyRepository = propertyRepository;
        this.reservationRepository = reservationRepository;
        this.authorizationService = authorizationService;
        this.propertyPurchaseOrchestrator = propertyPurchaseOrchestrator;
        this.idempotencyService = idempotencyService;
    }

    @Transactional
    public OfferEntity submit(UUID propertyId, SubmitOfferRequest request) {
        var actor = authorizationService.requireRole(AppRole.BUYER);
        PropertyEntity property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new NotFoundException("Property not found"));

        if (property.getStatus() != PropertyStatus.PUBLISHED) {
            throw new BadRequestException("Property must be published to receive offers");
        }

        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Offer amount must be positive");
        }

        OfferEntity offer = new OfferEntity();
        offer.setProperty(property);
        offer.setBuyerUserId(actor.userId());
        offer.setSellerUserId(property.getOwnerUserId());
        offer.setAmount(request.amount());
        offer.setCurrency(request.currency());
        offer.setStatus(OfferStatus.SUBMITTED);
        offer.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));

        OfferEntity saved = offerRepository.save(offer);
        appendEvent(saved, "SUBMITTED", actor.userId(), request.notes());
        return saved;
    }

    @Transactional
    public OfferEntity counter(UUID offerId, CounterOfferRequest request) {
        OfferEntity offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new NotFoundException("Offer not found"));

        var actor = authorizationService.requireSeller(offer.getSellerUserId());

        if (offer.getStatus() != OfferStatus.SUBMITTED && offer.getStatus() != OfferStatus.COUNTERED) {
            throw new BadRequestException("Offer cannot be countered in current status");
        }
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Counter amount must be positive");
        }

        offer.setAmount(request.amount());
        offer.setStatus(OfferStatus.COUNTERED);
        offer.setExpiresAt(Instant.now().plus(7, ChronoUnit.DAYS));
        appendEvent(offer, "COUNTERED", actor.userId(), request.notes());
        return offer;
    }

    @Transactional
    public AcceptOfferResult accept(UUID offerId, AcceptOfferRequest request) {
        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
            throw new BadRequestException("idempotencyKey is required");
        }

        OfferEntity offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new NotFoundException("Offer not found"));

        PropertyEntity property = propertyRepository.findByIdForUpdate(offer.getProperty().getId())
                .orElseThrow(() -> new NotFoundException("Property not found"));

        var actor = authorizationService.requireSeller(property.getOwnerUserId());
        String fingerprint = offerId + ":" + property.getId() + ":" + actor.userId();
        var reserved = idempotencyService.reserve("ACCEPT_OFFER", request.idempotencyKey(), fingerprint);

        if (!reserved.created()) {
            PropertyReservationEntity existing = reservationRepository
                    .findByOffer_IdAndStatus(offerId, ReservationStatus.ACTIVE)
                    .orElseThrow(() -> new ConflictException("Offer accept already processed but reservation missing"));
            return new AcceptOfferResult(offer, existing, false);
        }

        if (offer.getStatus() == OfferStatus.ACCEPTED) {
            PropertyReservationEntity existing = reservationRepository
                    .findByOffer_IdAndStatus(offerId, ReservationStatus.ACTIVE)
                    .orElseThrow(() -> new ConflictException("Accepted offer missing active reservation"));
            return new AcceptOfferResult(offer, existing, true);
        }

        if (offer.getStatus() == OfferStatus.WITHDRAWN || offer.getStatus() == OfferStatus.REJECTED || offer.getStatus() == OfferStatus.EXPIRED) {
            throw new BadRequestException("Offer cannot be accepted in current status");
        }

        reservationRepository.findByProperty_IdAndStatus(property.getId(), ReservationStatus.ACTIVE)
                .ifPresent(existing -> {
                    if (!existing.getOffer().getId().equals(offerId)) {
                        throw new ConflictException("Property already has an active reservation");
                    }
                });

        offerRepository.findByProperty_IdAndStatus(property.getId(), OfferStatus.ACCEPTED)
                .ifPresent(existing -> {
                    if (!existing.getId().equals(offerId)) {
                        throw new ConflictException("Property already has an accepted offer");
                    }
                });

        offer.setStatus(OfferStatus.ACCEPTED);
        appendEvent(offer, "ACCEPTED", actor.userId(), request.notes());

        List<OfferEntity> openOffers = offerRepository.findByProperty_IdAndStatusIn(
                property.getId(),
                List.of(OfferStatus.SUBMITTED, OfferStatus.COUNTERED)
        );
        for (OfferEntity other : openOffers) {
            if (!other.getId().equals(offer.getId())) {
                other.setStatus(OfferStatus.REJECTED);
                appendEvent(other, "AUTO_REJECTED", actor.userId(), "Another offer accepted");
            }
        }

        PropertyReservationEntity reservation = new PropertyReservationEntity();
        reservation.setProperty(property);
        reservation.setOffer(offer);
        reservation.setBuyerUserId(offer.getBuyerUserId());
        reservation.setSellerUserId(offer.getSellerUserId());
        reservation.setStatus(ReservationStatus.ACTIVE);
        reservation.setReservedUntil(Instant.now().plus(72, ChronoUnit.HOURS));
        PropertyReservationEntity savedReservation = reservationRepository.save(reservation);

        propertyPurchaseOrchestrator.createPurchaseEscrow(savedReservation, offer);
        return new AcceptOfferResult(offer, savedReservation, true);
    }

    @Transactional
    public OfferEntity reject(UUID offerId, String notes) {
        OfferEntity offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new NotFoundException("Offer not found"));
        var actor = authorizationService.requireSeller(offer.getSellerUserId());

        if (offer.getStatus() == OfferStatus.ACCEPTED) {
            throw new BadRequestException("Cannot reject accepted offer");
        }
        if (offer.getStatus() == OfferStatus.REJECTED || offer.getStatus() == OfferStatus.WITHDRAWN || offer.getStatus() == OfferStatus.EXPIRED) {
            return offer;
        }

        offer.setStatus(OfferStatus.REJECTED);
        appendEvent(offer, "REJECTED", actor.userId(), notes);
        return offer;
    }

    @Transactional
    public OfferEntity withdraw(UUID offerId, String notes) {
        OfferEntity offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new NotFoundException("Offer not found"));
        var actor = authorizationService.requireBuyer(offer.getBuyerUserId());

        if (offer.getStatus() == OfferStatus.ACCEPTED) {
            throw new BadRequestException("Cannot withdraw accepted offer");
        }

        offer.setStatus(OfferStatus.WITHDRAWN);
        appendEvent(offer, "WITHDRAWN", actor.userId(), notes);
        return offer;
    }

    @Transactional
    public PropertyReservationEntity cancelReservation(UUID reservationId, String notes) {
        PropertyReservationEntity reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reservation not found"));
        var actor = authorizationService.requireSeller(reservation.getSellerUserId());

        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            return reservation;
        }

        reservation.setStatus(ReservationStatus.CANCELLED);
        OfferEntity offer = reservation.getOffer();
        if (offer.getStatus() == OfferStatus.ACCEPTED) {
            offer.setStatus(OfferStatus.REJECTED);
            appendEvent(offer, "RESERVATION_CANCELLED", actor.userId(), notes);
        }

        return reservation;
    }

    private void appendEvent(OfferEntity offer, String eventType, UUID actorUserId, String notes) {
        OfferEventEntity event = new OfferEventEntity();
        event.setOffer(offer);
        event.setEventType(eventType);
        event.setActorUserId(actorUserId);
        event.setNotes(notes);
        offerEventRepository.save(event);
    }

    public record AcceptOfferResult(OfferEntity offer, PropertyReservationEntity reservation, boolean createdNow) {
    }
}
