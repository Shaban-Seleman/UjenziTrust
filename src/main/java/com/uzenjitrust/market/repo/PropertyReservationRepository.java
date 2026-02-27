package com.uzenjitrust.market.repo;

import com.uzenjitrust.market.domain.PropertyReservationEntity;
import com.uzenjitrust.market.domain.ReservationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PropertyReservationRepository extends JpaRepository<PropertyReservationEntity, UUID> {

    Page<PropertyReservationEntity> findByBuyerUserIdOrSellerUserId(UUID buyerUserId, UUID sellerUserId, Pageable pageable);

    Optional<PropertyReservationEntity> findByProperty_IdAndStatus(UUID propertyId, ReservationStatus status);

    Optional<PropertyReservationEntity> findByOffer_IdAndStatus(UUID offerId, ReservationStatus status);

    @Modifying
    @Query(value = """
        update market.property_reservations
        set status = 'EXPIRED', updated_at = now()
        where status = 'ACTIVE'
          and reserved_until is not null
          and reserved_until < :now
        """, nativeQuery = true)
    int expireReservations(Instant now);
}
