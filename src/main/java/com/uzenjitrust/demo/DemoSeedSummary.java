package com.uzenjitrust.demo;

public record DemoSeedSummary(
        int usersCreated,
        int propertiesCreated,
        int offersCreated,
        int escrowsCreated,
        int projectsCreated,
        int milestonesCreated,
        int disbursementsCreated,
        int settledDisbursements,
        int retentionReadyCount
) {
}
