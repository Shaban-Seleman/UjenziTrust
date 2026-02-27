package com.uzenjitrust.market.repo;

import com.uzenjitrust.market.domain.OfferEntity;
import com.uzenjitrust.market.domain.OfferStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<OfferEntity, UUID> {

    Page<OfferEntity> findByBuyerUserIdOrSellerUserId(UUID buyerUserId, UUID sellerUserId, Pageable pageable);

    Optional<OfferEntity> findByProperty_IdAndStatus(UUID propertyId, OfferStatus status);

    List<OfferEntity> findByProperty_IdAndStatusIn(UUID propertyId, List<OfferStatus> statuses);

    @Modifying
    @Query(value = """
        update market.offers
        set status = 'EXPIRED', updated_at = now()
        where status in ('SUBMITTED', 'COUNTERED')
          and expires_at is not null
          and expires_at < :now
        """, nativeQuery = true)
    int expireOffers(Instant now);
}
