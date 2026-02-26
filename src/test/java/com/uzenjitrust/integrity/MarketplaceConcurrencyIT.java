package com.uzenjitrust.integrity;

import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.market.api.AcceptOfferRequest;
import com.uzenjitrust.market.domain.OfferEntity;
import com.uzenjitrust.market.domain.PropertyEntity;
import com.uzenjitrust.market.service.OfferService;
import com.uzenjitrust.support.TestSecurity;
import com.uzenjitrust.support.integrity.ConcurrencyTestHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("verify")
class MarketplaceConcurrencyIT extends AbstractFinancialIntegrityIT {

    @Test
    void acceptOfferConcurrentOnlyOneWins() {
        UUID seller = UUID.randomUUID();
        UUID buyerA = UUID.randomUUID();
        UUID buyerB = UUID.randomUUID();

        PropertyEntity property = dataFactory.property(seller, "Concurrency Property", new BigDecimal("120000000"));
        OfferEntity offerA = dataFactory.offer(property, buyerA, new BigDecimal("110000000"));
        OfferEntity offerB = dataFactory.offer(property, buyerB, new BigDecimal("115000000"));

        AtomicInteger turn = new AtomicInteger();
        List<ConcurrencyTestHelper.Result<OfferService.AcceptOfferResult>> results = ConcurrencyTestHelper.runConcurrently(2, () -> {
            TestSecurity.as(seller, AppRole.SELLER);
            if (turn.getAndIncrement() == 0) {
                return offerService.accept(offerA.getId(), new AcceptOfferRequest("CONCUR-ACC-A", "accept A"));
            }
            return offerService.accept(offerB.getId(), new AcceptOfferRequest("CONCUR-ACC-B", "accept B"));
        });

        long successes = results.stream().filter(ConcurrencyTestHelper.Result::isSuccess).count();
        long failures = results.stream().filter(r -> !r.isSuccess()).count();
        assertEquals(1L, successes);
        assertEquals(1L, failures);

        assertEquals(1L, assertions.activeReservationCount(property.getId()));
        assertEquals(1L, assertions.acceptedOfferCount(property.getId()));
    }
}
