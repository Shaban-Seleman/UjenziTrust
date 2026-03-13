package com.uzenjitrust.demo;

import com.uzenjitrust.common.error.BadRequestException;
import com.uzenjitrust.common.error.ForbiddenException;
import com.uzenjitrust.common.security.AuthorizationService;
import com.uzenjitrust.ops.service.OperatorAuditService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Profile("local")
@ConditionalOnProperty(prefix = "app.demo", name = "enabled", havingValue = "true")
@RequestMapping("/admin/demo")
public class DemoAdminController {

    private static final String INVESTOR_SCENARIO = "investor_v1";

    private final DemoSeeder demoSeeder;
    private final AuthorizationService authorizationService;
    private final OperatorAuditService operatorAuditService;

    public DemoAdminController(DemoSeeder demoSeeder,
                               AuthorizationService authorizationService,
                               OperatorAuditService operatorAuditService) {
        this.demoSeeder = demoSeeder;
        this.authorizationService = authorizationService;
        this.operatorAuditService = operatorAuditService;
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset() {
        try {
            authorizationService.requireAdmin();
            demoSeeder.resetAll();
            operatorAuditService.recordSuccess("DEMO_RESET", "DEMO_SCENARIO", INVESTOR_SCENARIO, "Demo data reset", Map.of("scenario", INVESTOR_SCENARIO));
            return ResponseEntity.ok(Map.of("status", "ok"));
        } catch (ForbiddenException ex) {
            operatorAuditService.recordForbidden("DEMO_RESET", "DEMO_SCENARIO", INVESTOR_SCENARIO, "Demo reset requires admin", Map.of("scenario", INVESTOR_SCENARIO), ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            operatorAuditService.recordFailure("DEMO_RESET", "DEMO_SCENARIO", INVESTOR_SCENARIO, "Demo reset failed", Map.of("scenario", INVESTOR_SCENARIO), ex.getMessage());
            throw ex;
        }
    }

    @PostMapping("/seed")
    public ResponseEntity<DemoSeedSummary> seed(@RequestParam(defaultValue = INVESTOR_SCENARIO) String scenario) {
        try {
            authorizationService.requireAdmin();
            validateScenario(scenario);
            DemoSeedSummary summary = demoSeeder.seedInvestorScenario();
            operatorAuditService.recordSuccess("DEMO_SEED", "DEMO_SCENARIO", scenario, "Demo scenario seeded", seedMetadata(scenario, summary));
            return ResponseEntity.ok(summary);
        } catch (ForbiddenException ex) {
            operatorAuditService.recordForbidden("DEMO_SEED", "DEMO_SCENARIO", scenario, "Demo seed requires admin", Map.of("scenario", scenario), ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            operatorAuditService.recordFailure("DEMO_SEED", "DEMO_SCENARIO", scenario, "Demo seed failed", Map.of("scenario", scenario), ex.getMessage());
            throw ex;
        }
    }

    @PostMapping("/reset-and-seed")
    public ResponseEntity<DemoSeedSummary> resetAndSeed(@RequestParam(defaultValue = INVESTOR_SCENARIO) String scenario) {
        try {
            authorizationService.requireAdmin();
            validateScenario(scenario);
            demoSeeder.resetAll();
            DemoSeedSummary summary = demoSeeder.seedInvestorScenario();
            operatorAuditService.recordSuccess("DEMO_RESET_AND_SEED", "DEMO_SCENARIO", scenario, "Demo reset and seed completed", seedMetadata(scenario, summary));
            return ResponseEntity.ok(summary);
        } catch (ForbiddenException ex) {
            operatorAuditService.recordForbidden("DEMO_RESET_AND_SEED", "DEMO_SCENARIO", scenario, "Demo reset-and-seed requires admin", Map.of("scenario", scenario), ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            operatorAuditService.recordFailure("DEMO_RESET_AND_SEED", "DEMO_SCENARIO", scenario, "Demo reset-and-seed failed", Map.of("scenario", scenario), ex.getMessage());
            throw ex;
        }
    }

    private void validateScenario(String scenario) {
        if (!INVESTOR_SCENARIO.equals(scenario)) {
            throw new BadRequestException("Unsupported demo scenario");
        }
    }

    private Map<String, Object> seedMetadata(String scenario, DemoSeedSummary summary) {
        return Map.of(
                "scenario", scenario,
                "usersCreated", summary.usersCreated(),
                "propertiesCreated", summary.propertiesCreated(),
                "offersCreated", summary.offersCreated(),
                "escrowsCreated", summary.escrowsCreated(),
                "projectsCreated", summary.projectsCreated(),
                "milestonesCreated", summary.milestonesCreated(),
                "disbursementsCreated", summary.disbursementsCreated(),
                "settledDisbursements", summary.settledDisbursements(),
                "retentionReadyCount", summary.retentionReadyCount()
        );
    }
}
