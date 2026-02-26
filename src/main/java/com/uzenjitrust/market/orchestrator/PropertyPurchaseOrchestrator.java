package com.uzenjitrust.market.orchestrator;

import com.uzenjitrust.market.domain.OfferEntity;
import com.uzenjitrust.market.domain.PropertyReservationEntity;
import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.service.EscrowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PropertyPurchaseOrchestrator {

    private final EscrowService escrowService;

    public PropertyPurchaseOrchestrator(EscrowService escrowService) {
        this.escrowService = escrowService;
    }

    @Transactional
    public EscrowEntity createPurchaseEscrow(PropertyReservationEntity reservation, OfferEntity offer) {
        String businessKey = "PROPERTY_PURCHASE:" + reservation.getId();
        return escrowService.createEscrowIdempotent(
                businessKey,
                "PROPERTY_PURCHASE",
                offer.getAmount(),
                offer.getCurrency(),
                offer.getBuyerUserId(),
                offer.getSellerUserId()
        );
    }
}
