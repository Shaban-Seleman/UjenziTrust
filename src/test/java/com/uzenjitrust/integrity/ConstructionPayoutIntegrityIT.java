package com.uzenjitrust.integrity;

import com.uzenjitrust.build.api.ApproveMilestoneMultiRequest;
import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.ops.domain.DisbursementOrderEntity;
import com.uzenjitrust.support.TestSecurity;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("verify")
class ConstructionPayoutIntegrityIT extends AbstractFinancialIntegrityIT {

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
}
