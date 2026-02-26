package com.uzenjitrust.market.api;

import com.uzenjitrust.market.domain.PropertyEntity;
import com.uzenjitrust.market.domain.PropertyStatus;
import com.uzenjitrust.market.service.PropertyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/market/properties")
@Tag(name = "Marketplace Properties")
public class PropertyController {

    private final PropertyService propertyService;

    public PropertyController(PropertyService propertyService) {
        this.propertyService = propertyService;
    }

    @PostMapping
    @Operation(summary = "Create draft property")
    public ResponseEntity<PropertyEntity> create(@Valid @RequestBody CreatePropertyRequest request) {
        return ResponseEntity.ok(propertyService.createDraft(request));
    }

    @PutMapping("/{propertyId}")
    @Operation(summary = "Update draft/published property")
    public ResponseEntity<PropertyEntity> update(@PathVariable UUID propertyId,
                                                 @RequestBody UpdatePropertyRequest request) {
        return ResponseEntity.ok(propertyService.update(propertyId, request));
    }

    @PostMapping("/{propertyId}/publish")
    @Operation(summary = "Publish property")
    public ResponseEntity<PropertyEntity> publish(@PathVariable UUID propertyId) {
        return ResponseEntity.ok(propertyService.publish(propertyId));
    }

    @GetMapping
    @Operation(summary = "Search published properties")
    public ResponseEntity<Page<PropertyEntity>> search(
            @RequestParam(defaultValue = "PUBLISHED") PropertyStatus status,
            @RequestParam(defaultValue = "0") BigDecimal minPrice,
            @RequestParam(defaultValue = "999999999") BigDecimal maxPrice,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return ResponseEntity.ok(propertyService.search(status, minPrice, maxPrice, page, size, sortBy, direction));
    }
}
