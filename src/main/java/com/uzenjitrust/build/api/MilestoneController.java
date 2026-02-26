package com.uzenjitrust.build.api;

import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneSubmissionEntity;
import com.uzenjitrust.build.orchestrator.MilestoneOrchestrator;
import com.uzenjitrust.build.orchestrator.MultiPartyMilestoneOrchestrator;
import com.uzenjitrust.build.orchestrator.RetentionOrchestrator;
import com.uzenjitrust.build.service.MilestoneService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/build")
@Tag(name = "Build Milestones")
public class MilestoneController {

    private final MilestoneService milestoneService;
    private final MilestoneOrchestrator milestoneOrchestrator;
    private final MultiPartyMilestoneOrchestrator multiOrchestrator;
    private final RetentionOrchestrator retentionOrchestrator;

    public MilestoneController(MilestoneService milestoneService,
                               MilestoneOrchestrator milestoneOrchestrator,
                               MultiPartyMilestoneOrchestrator multiOrchestrator,
                               RetentionOrchestrator retentionOrchestrator) {
        this.milestoneService = milestoneService;
        this.milestoneOrchestrator = milestoneOrchestrator;
        this.multiOrchestrator = multiOrchestrator;
        this.retentionOrchestrator = retentionOrchestrator;
    }

    @PostMapping("/projects/{projectId}/milestones")
    @Operation(summary = "Create milestone")
    public ResponseEntity<MilestoneEntity> create(@PathVariable UUID projectId,
                                                   @Valid @RequestBody CreateMilestoneRequest request) {
        return ResponseEntity.ok(milestoneService.create(projectId, request));
    }

    @GetMapping("/projects/{projectId}/milestones")
    @Operation(summary = "List milestones by project")
    public ResponseEntity<List<MilestoneEntity>> listByProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(milestoneService.listByProject(projectId));
    }

    @PostMapping("/milestones/{milestoneId}/submit")
    @Operation(summary = "Submit milestone evidence")
    public ResponseEntity<MilestoneSubmissionEntity> submitEvidence(@PathVariable UUID milestoneId,
                                                                    @Valid @RequestBody SubmitMilestoneEvidenceRequest request) {
        return ResponseEntity.ok(milestoneService.submitEvidence(milestoneId, request));
    }

    @PostMapping("/milestones/{milestoneId}/approve")
    @Operation(summary = "Approve milestone (single payout)")
    public ResponseEntity<MilestoneEntity> approveSingle(@PathVariable UUID milestoneId,
                                                         @Valid @RequestBody ApproveMilestoneRequest request) {
        return ResponseEntity.ok(milestoneOrchestrator.approveMilestoneSingle(milestoneId, request.idempotencyKey()));
    }

    @PostMapping("/milestones/{milestoneId}/approve-multi")
    @Operation(summary = "Approve milestone (multi-party payout)")
    public ResponseEntity<MilestoneEntity> approveMulti(@PathVariable UUID milestoneId,
                                                        @Valid @RequestBody ApproveMilestoneMultiRequest request) {
        return ResponseEntity.ok(multiOrchestrator.approveMilestoneMulti(milestoneId, request));
    }

    @PostMapping("/milestones/{milestoneId}/retention-release")
    @Operation(summary = "Release milestone retention")
    public ResponseEntity<MilestoneEntity> releaseRetention(@PathVariable UUID milestoneId,
                                                            @Valid @RequestBody ReleaseRetentionRequest request) {
        return ResponseEntity.ok(retentionOrchestrator.releaseRetention(milestoneId, request.idempotencyKey()));
    }
}
