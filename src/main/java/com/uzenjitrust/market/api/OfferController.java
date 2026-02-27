package com.uzenjitrust.market.api;

import com.uzenjitrust.market.domain.OfferEntity;
import com.uzenjitrust.market.domain.PropertyReservationEntity;
import com.uzenjitrust.market.service.MarketReadService;
import com.uzenjitrust.market.service.OfferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/market")
@Tag(name = "Marketplace Offers")
public class OfferController {

    private final OfferService offerService;
    private final MarketReadService marketReadService;

    public OfferController(OfferService offerService,
                           MarketReadService marketReadService) {
        this.offerService = offerService;
        this.marketReadService = marketReadService;
    }

    @GetMapping("/offers")
    @Operation(summary = "List offers visible to current actor")
    public ResponseEntity<Page<OfferEntity>> listOffers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return ResponseEntity.ok(marketReadService.listOffers(page, size, sortBy, direction));
    }

    @GetMapping("/reservations")
    @Operation(summary = "List reservations visible to current actor")
    public ResponseEntity<Page<PropertyReservationEntity>> listReservations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt") String sortBy,
            @RequestParam(defaultValue = "DESC") Sort.Direction direction) {
        return ResponseEntity.ok(marketReadService.listReservations(page, size, sortBy, direction));
    }

    @PostMapping("/properties/{propertyId}/offers")
    @Operation(summary = "Submit offer")
    public ResponseEntity<OfferEntity> submit(@PathVariable UUID propertyId,
                                              @Valid @RequestBody SubmitOfferRequest request) {
        return ResponseEntity.ok(offerService.submit(propertyId, request));
    }

    @PostMapping("/offers/{offerId}/counter")
    @Operation(summary = "Counter offer")
    public ResponseEntity<OfferEntity> counter(@PathVariable UUID offerId,
                                               @Valid @RequestBody CounterOfferRequest request) {
        return ResponseEntity.ok(offerService.counter(offerId, request));
    }

    @PostMapping("/offers/{offerId}/accept")
    @Operation(summary = "Accept offer and create reservation + escrow")
    public ResponseEntity<OfferService.AcceptOfferResult> accept(@PathVariable UUID offerId,
                                                                 @Valid @RequestBody AcceptOfferRequest request) {
        return ResponseEntity.ok(offerService.accept(offerId, request));
    }

    @PostMapping("/offers/{offerId}/reject")
    @Operation(summary = "Reject offer")
    public ResponseEntity<OfferEntity> reject(@PathVariable UUID offerId,
                                              @RequestBody(required = false) NoteRequest request) {
        return ResponseEntity.ok(offerService.reject(offerId, request == null ? null : request.notes()));
    }

    @PostMapping("/offers/{offerId}/withdraw")
    @Operation(summary = "Withdraw offer")
    public ResponseEntity<OfferEntity> withdraw(@PathVariable UUID offerId,
                                                @RequestBody(required = false) NoteRequest request) {
        return ResponseEntity.ok(offerService.withdraw(offerId, request == null ? null : request.notes()));
    }

    @PostMapping("/reservations/{reservationId}/cancel")
    @Operation(summary = "Cancel active reservation")
    public ResponseEntity<PropertyReservationEntity> cancelReservation(@PathVariable UUID reservationId,
                                                                       @RequestBody(required = false) NoteRequest request) {
        return ResponseEntity.ok(offerService.cancelReservation(reservationId, request == null ? null : request.notes()));
    }
}
