package com.uzenjitrust.demo;

import com.uzenjitrust.common.error.BadRequestException;
import com.uzenjitrust.common.security.AuthorizationService;
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

    public DemoAdminController(DemoSeeder demoSeeder,
                               AuthorizationService authorizationService) {
        this.demoSeeder = demoSeeder;
        this.authorizationService = authorizationService;
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> reset() {
        authorizationService.requireAdmin();
        demoSeeder.resetAll();
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/seed")
    public ResponseEntity<DemoSeedSummary> seed(@RequestParam(defaultValue = INVESTOR_SCENARIO) String scenario) {
        authorizationService.requireAdmin();
        validateScenario(scenario);
        return ResponseEntity.ok(demoSeeder.seedInvestorScenario());
    }

    @PostMapping("/reset-and-seed")
    public ResponseEntity<DemoSeedSummary> resetAndSeed(@RequestParam(defaultValue = INVESTOR_SCENARIO) String scenario) {
        authorizationService.requireAdmin();
        validateScenario(scenario);
        demoSeeder.resetAll();
        return ResponseEntity.ok(demoSeeder.seedInvestorScenario());
    }

    private void validateScenario(String scenario) {
        if (!INVESTOR_SCENARIO.equals(scenario)) {
            throw new BadRequestException("Unsupported demo scenario");
        }
    }
}
