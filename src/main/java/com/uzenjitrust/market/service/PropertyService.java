package com.uzenjitrust.market.service;

import com.uzenjitrust.common.error.BadRequestException;
import com.uzenjitrust.common.error.NotFoundException;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.common.security.AuthorizationService;
import com.uzenjitrust.market.api.CreatePropertyRequest;
import com.uzenjitrust.market.api.UpdatePropertyRequest;
import com.uzenjitrust.market.domain.PropertyEntity;
import com.uzenjitrust.market.domain.PropertyStatus;
import com.uzenjitrust.market.repo.PropertyRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final AuthorizationService authorizationService;

    public PropertyService(PropertyRepository propertyRepository,
                           AuthorizationService authorizationService) {
        this.propertyRepository = propertyRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional
    public PropertyEntity createDraft(CreatePropertyRequest request) {
        var actor = authorizationService.requireRole(AppRole.OWNER, AppRole.SELLER);
        PropertyEntity property = new PropertyEntity();
        property.setOwnerUserId(actor.userId());
        property.setTitle(request.title());
        property.setDescription(request.description());
        property.setLocation(request.location());
        property.setAskingPrice(request.askingPrice());
        property.setCurrency(request.currency());
        property.setStatus(PropertyStatus.DRAFT);
        return propertyRepository.save(property);
    }

    @Transactional
    public PropertyEntity update(UUID propertyId, UpdatePropertyRequest request) {
        PropertyEntity property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new NotFoundException("Property not found"));
        requireOwnerOrSeller(property.getOwnerUserId());
        if (request.title() != null) {
            property.setTitle(request.title());
        }
        if (request.description() != null) {
            property.setDescription(request.description());
        }
        if (request.location() != null) {
            property.setLocation(request.location());
        }
        if (request.askingPrice() != null) {
            property.setAskingPrice(request.askingPrice());
        }
        if (request.currency() != null) {
            property.setCurrency(request.currency());
        }
        return property;
    }

    @Transactional
    public PropertyEntity publish(UUID propertyId) {
        PropertyEntity property = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new NotFoundException("Property not found"));
        requireOwnerOrSeller(property.getOwnerUserId());

        if (property.getTitle() == null || property.getTitle().isBlank()) {
            throw new BadRequestException("Property title is required");
        }
        if (property.getAskingPrice() == null || property.getAskingPrice().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Asking price must be positive");
        }

        property.setStatus(PropertyStatus.PUBLISHED);
        property.setPublishedAt(Instant.now());
        return property;
    }

    @Transactional(readOnly = true)
    public Page<PropertyEntity> search(PropertyStatus status,
                                       BigDecimal minPrice,
                                       BigDecimal maxPrice,
                                       int page,
                                       int size,
                                       String sortBy,
                                       Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return propertyRepository.findByStatusAndAskingPriceBetween(status, minPrice, maxPrice, pageable);
    }

    private void requireOwnerOrSeller(UUID ownerUserId) {
        var actor = authorizationService.requireRole(AppRole.OWNER, AppRole.SELLER);
        if (!actor.userId().equals(ownerUserId)) {
            throw new com.uzenjitrust.common.error.ForbiddenException("Actor is not property owner/seller");
        }
    }
}
