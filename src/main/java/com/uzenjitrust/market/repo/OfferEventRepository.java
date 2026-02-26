package com.uzenjitrust.market.repo;

import com.uzenjitrust.market.domain.OfferEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OfferEventRepository extends JpaRepository<OfferEventEntity, UUID> {
}
