package com.uzenjitrust.integrity;

import com.uzenjitrust.build.api.ApproveMilestoneMultiRequest;
import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.ledger.service.LedgerAccountCodes;
import com.uzenjitrust.ops.domain.DisbursementOrderEntity;
import com.uzenjitrust.support.TestSecurity;
import com.uzenjitrust.support.integrity.ConcurrencyTestHelper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("verify")
class ConstructionPayoutIntegrityIT extends AbstractFinancialIntegrityIT {

    @Test
    void milestoneAuthorizationCreditsPayablesByPayeeType() {
        UUID owner = UUID.randomUUID();
        UUID contractor = UUID.randomUUID();
        UUID supplier = UUID.randomUUID();
        UUID inspector = UUID.randomUUID();

        ProjectEntity project = dataFactory.projectWithEscrow(owner, contractor, inspector, "payee-mapping", new BigDecimal("7000000"));
        MilestoneEntity milestone = dataFactory.milestone(
                project,
                1,
                new BigDecimal("1000000"),
                new BigDecimal("100000"),
                MilestoneStatus.SUBMITTED
        );

        TestSecurity.as(owner, AppRole.OWNER);
        multiPartyMilestoneOrchestrator.approveMilestoneMulti(milestone.getId(), new ApproveMilestoneMultiRequest(
                "MULTI-MAP-1",
                List.of(
                        new ApproveMilestoneMultiRequest.Split("CONTRACTOR", contractor, new BigDecimal("500000"), "SPLIT-MAP-A"),
                        new ApproveMilestoneMultiRequest.Split("SUPPLIER", supplier, new BigDecimal("200000"), "SPLIT-MAP-B"),
                        new ApproveMilestoneMultiRequest.Split("INSPECTOR", inspector, new BigDecimal("200000"), "SPLIT-MAP-C")
                )
        ));

        String referenceId = milestone.getId().toString();
        assertEquals(0, new BigDecimal("500000").compareTo(assertions.ledgerLineAmountByEntryAccountAndType(
                "CONSTRUCTION_MILESTONE_AUTHORIZED",
                referenceId,
                LedgerAccountCodes.PAYABLE_CONTRACTOR,
                "CREDIT"
        )));
        assertEquals(0, new BigDecimal("200000").compareTo(assertions.ledgerLineAmountByEntryAccountAndType(
                "CONSTRUCTION_MILESTONE_AUTHORIZED",
                referenceId,
                LedgerAccountCodes.PAYABLE_SUPPLIER,
                "CREDIT"
        )));
        assertEquals(0, new BigDecimal("200000").compareTo(assertions.ledgerLineAmountByEntryAccountAndType(
                "CONSTRUCTION_MILESTONE_AUTHORIZED",
                referenceId,
                LedgerAccountCodes.PAYABLE_INSPECTOR,
                "CREDIT"
        )));
        assertEquals(0, new BigDecimal("100000").compareTo(assertions.ledgerLineAmountByEntryAccountAndType(
                "CONSTRUCTION_MILESTONE_AUTHORIZED",
                referenceId,
                LedgerAccountCodes.PAYABLE_RETENTION,
                "CREDIT"
        )));
    }

    @Test
    void milestonePaidOnlyAfterAllSplitDisbursementsSettled() throws Exception {
        UUID owner = UUID.randomUUID();
        UUID contractor = UUID.randomUUID();
        UUID inspector = UUID.randomUUID();

        ProjectEntity project = dataFactory.projectWithEscrow(owner, contractor, inspector, "multi-split", new BigDecimal("7000000"));
        MilestoneEntity milestone = dataFactory.milestone(
                project,
                1,
                new BigDecimal("1000000"),
                new BigDecimal("100000"),
                MilestoneStatus.SUBMITTED
        );

        TestSecurity.as(owner, AppRole.OWNER);
        multiPartyMilestoneOrchestrator.approveMilestoneMulti(milestone.getId(), new ApproveMilestoneMultiRequest(
                "MULTI-IDEMP-1",
                List.of(
                        new ApproveMilestoneMultiRequest.Split("CONTRACTOR", contractor, new BigDecimal("300000"), "SPLIT-A"),
                        new ApproveMilestoneMultiRequest.Split("CONTRACTOR", contractor, new BigDecimal("300000"), "SPLIT-B"),
                        new ApproveMilestoneMultiRequest.Split("INSPECTOR", inspector, new BigDecimal("300000"), "SPLIT-C")
                )
        ));

        List<DisbursementOrderEntity> splits = disbursementOrderRepository.findByMilestoneId(milestone.getId());
        assertEquals(3, splits.size());

        settleByWebhook(splits.get(0).getId(), "MSPLIT-EVT-1", "MSPLIT-SETTLE-1");
        MilestoneEntity afterOne = milestoneRepository.findById(milestone.getId()).orElseThrow();
        assertEquals(MilestoneStatus.APPROVED, afterOne.getStatus());
        assertEquals(2L, assertions.unsettledDisbursementCount(milestone.getId()));

        settleByWebhook(splits.get(1).getId(), "MSPLIT-EVT-2", "MSPLIT-SETTLE-2");
        settleByWebhook(splits.get(2).getId(), "MSPLIT-EVT-3", "MSPLIT-SETTLE-3");

        MilestoneEntity paid = milestoneRepository.findById(milestone.getId()).orElseThrow();
        assertEquals(MilestoneStatus.PAID, paid.getStatus());
        assertNotNull(paid.getPaidAt());
        assertNotNull(paid.getRetentionReleaseAt());
        assertEquals(0L, assertions.unsettledDisbursementCount(milestone.getId()));
    }

    @Test
    void concurrentSettlementWebhooksMarkMilestonePaidExactlyOnce() {
        UUID owner = UUID.randomUUID();
        UUID contractor = UUID.randomUUID();
        UUID supplier = UUID.randomUUID();
        UUID inspector = UUID.randomUUID();

        ProjectEntity project = dataFactory.projectWithEscrow(owner, contractor, inspector, "settlement-race", new BigDecimal("7000000"));
        MilestoneEntity milestone = dataFactory.milestone(
                project,
                1,
                new BigDecimal("1000000"),
                new BigDecimal("100000"),
                MilestoneStatus.SUBMITTED
        );

        TestSecurity.as(owner, AppRole.OWNER);
        multiPartyMilestoneOrchestrator.approveMilestoneMulti(milestone.getId(), new ApproveMilestoneMultiRequest(
                "MULTI-CONCUR-1",
                List.of(
                        new ApproveMilestoneMultiRequest.Split("CONTRACTOR", contractor, new BigDecimal("400000"), "SPLIT-CON-A"),
                        new ApproveMilestoneMultiRequest.Split("SUPPLIER", supplier, new BigDecimal("300000"), "SPLIT-CON-B"),
                        new ApproveMilestoneMultiRequest.Split("INSPECTOR", inspector, new BigDecimal("200000"), "SPLIT-CON-C")
                )
        ));

        List<DisbursementOrderEntity> splits = new ArrayList<>(disbursementOrderRepository.findByMilestoneId(milestone.getId()));
        splits.sort(Comparator.comparing(DisbursementOrderEntity::getId));
        assertEquals(3, splits.size());

        AtomicInteger idx = new AtomicInteger();
        List<ConcurrencyTestHelper.Result<String>> results = ConcurrencyTestHelper.runConcurrently(3, () -> {
            int i = idx.getAndIncrement();
            DisbursementOrderEntity disbursement = splits.get(i);
            return settleByWebhook(disbursement.getId(), "CONCUR-EVT-" + i, "CONCUR-SETTLE-" + i);
        });

        assertTrue(results.stream().allMatch(ConcurrencyTestHelper.Result::isSuccess));
        results.forEach(result -> assertEquals("PROCESSED", result.value()));

        MilestoneEntity paid = milestoneRepository.findById(milestone.getId()).orElseThrow();
        assertEquals(MilestoneStatus.PAID, paid.getStatus());
        assertNotNull(paid.getPaidAt());
        assertNotNull(paid.getRetentionReleaseAt());
        assertNull(paid.getRetentionReleasedAt());
        assertEquals(0L, assertions.unsettledDisbursementCount(milestone.getId()));
        for (DisbursementOrderEntity split : splits) {
            assertEquals(1L, assertions.ledgerLineCountByEntryAndAccount(
                    "BANK_PAYOUT_SETTLED",
                    split.getId().toString(),
                    LedgerAccountCodes.payableForPayeeType(split.getPayeeType())
            ));
        }
    }
}
