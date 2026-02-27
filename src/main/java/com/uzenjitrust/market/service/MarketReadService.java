package com.uzenjitrust.market.service;

import com.uzenjitrust.common.error.ForbiddenException;
import com.uzenjitrust.common.security.ActorProvider;
import com.uzenjitrust.common.security.ActorPrincipal;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.market.domain.OfferEntity;
import com.uzenjitrust.market.domain.PropertyReservationEntity;
import com.uzenjitrust.market.repo.OfferRepository;
import com.uzenjitrust.market.repo.PropertyReservationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketReadService {

    private final OfferRepository offerRepository;
    private final PropertyReservationRepository reservationRepository;
    private final ActorProvider actorProvider;

    public MarketReadService(OfferRepository offerRepository,
                             PropertyReservationRepository reservationRepository,
                             ActorProvider actorProvider) {
        this.offerRepository = offerRepository;
        this.reservationRepository = reservationRepository;
        this.actorProvider = actorProvider;
    }

    @Transactional(readOnly = true)
    public Page<OfferEntity> listOffers(int page, int size, String sortBy, Sort.Direction direction) {
        ActorPrincipal actor = actorProvider.requireActor();
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        if (isAdmin(actor)) {
            return offerRepository.findAll(pageable);
        }
        requireBuyerOrSeller(actor);
        return offerRepository.findByBuyerUserIdOrSellerUserId(actor.userId(), actor.userId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<PropertyReservationEntity> listReservations(int page, int size, String sortBy, Sort.Direction direction) {
        ActorPrincipal actor = actorProvider.requireActor();
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        if (isAdmin(actor)) {
            return reservationRepository.findAll(pageable);
        }
        requireBuyerOrSeller(actor);
        return reservationRepository.findByBuyerUserIdOrSellerUserId(actor.userId(), actor.userId(), pageable);
    }

    private static boolean isAdmin(ActorPrincipal actor) {
        return actor.roles().contains(AppRole.ADMIN);
    }

    private static void requireBuyerOrSeller(ActorPrincipal actor) {
        if (!actor.roles().contains(AppRole.BUYER) && !actor.roles().contains(AppRole.SELLER)) {
            throw new ForbiddenException("Insufficient role");
        }
    }
}
