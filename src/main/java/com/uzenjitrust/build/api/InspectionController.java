package com.uzenjitrust.build.api;

import com.uzenjitrust.build.domain.InspectionEntity;
import com.uzenjitrust.build.orchestrator.InspectionOrchestrator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/build/inspections")
@Tag(name = "Build Inspections")
public class InspectionController {

    private final InspectionOrchestrator inspectionOrchestrator;

    public InspectionController(InspectionOrchestrator inspectionOrchestrator) {
        this.inspectionOrchestrator = inspectionOrchestrator;
    }

    @PostMapping("/schedule")
    @Operation(summary = "Schedule inspection")
    public ResponseEntity<InspectionEntity> schedule(@Valid @RequestBody ScheduleInspectionRequest request) {
        return ResponseEntity.ok(inspectionOrchestrator.schedule(request));
    }

    @PostMapping("/{inspectionId}/complete")
    @Operation(summary = "Complete inspection and authorize inspector payout")
    public ResponseEntity<InspectionEntity> complete(@PathVariable UUID inspectionId,
                                                     @Valid @RequestBody CompleteInspectionRequest request) {
        return ResponseEntity.ok(inspectionOrchestrator.complete(inspectionId, request));
    }
}
