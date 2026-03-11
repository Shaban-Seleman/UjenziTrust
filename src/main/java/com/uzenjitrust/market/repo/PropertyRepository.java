package com.uzenjitrust.market.repo;

import com.uzenjitrust.market.domain.PropertyEntity;
import com.uzenjitrust.market.domain.PropertyStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<PropertyEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select p from PropertyEntity p where p.id = :id")
    Optional<PropertyEntity> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id);

    Page<PropertyEntity> findByStatusAndAskingPriceBetween(PropertyStatus status, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    Page<PropertyEntity> findByOwnerUserId(UUID ownerUserId, Pageable pageable);
}
