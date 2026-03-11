package com.uzenjitrust.integration;

import com.uzenjitrust.build.repo.MilestoneRepository;
import com.uzenjitrust.build.repo.ProjectRepository;
import com.uzenjitrust.demo.DemoSeedSummary;
import com.uzenjitrust.demo.DemoSeeder;
import com.uzenjitrust.market.repo.OfferRepository;
import com.uzenjitrust.market.repo.PropertyRepository;
import com.uzenjitrust.ops.repo.EscrowRepository;
import com.uzenjitrust.support.PostgresIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DemoSeederIntegrationTest extends PostgresIntegrationTest {

    @Autowired
    private DemoSeeder demoSeeder;
    @Autowired
    private PropertyRepository propertyRepository;
    @Autowired
    private OfferRepository offerRepository;
    @Autowired
    private EscrowRepository escrowRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private MilestoneRepository milestoneRepository;

    @AfterEach
    void tearDown() {
        demoSeeder.resetAll();
    }

    @Test
    void seedInvestorScenarioCreatesRepeatableDemoDataset() {
        demoSeeder.resetAll();

        DemoSeedSummary summary = demoSeeder.seedInvestorScenario();

        assertTrue(propertyRepository.count() >= 20, "expected at least 20 properties");
        assertTrue(offerRepository.count() >= 60, "expected at least 60 offers");
        assertTrue(escrowRepository.count() >= 10, "expected at least 10 escrows");
        assertEquals(3, projectRepository.count(), "expected exactly 3 projects");
        assertEquals(12, milestoneRepository.count(), "expected exactly 12 milestones");

        assertTrue(summary.propertiesCreated() >= 20);
        assertTrue(summary.offersCreated() >= 60);
        assertTrue(summary.escrowsCreated() >= 10);
        assertEquals(3, summary.projectsCreated());
        assertEquals(12, summary.milestonesCreated());
    }
}
