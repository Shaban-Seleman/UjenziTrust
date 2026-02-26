package com.uzenjitrust.build.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uzenjitrust.build.api.CompleteInspectionRequest;
import com.uzenjitrust.build.api.ScheduleInspectionRequest;
import com.uzenjitrust.build.domain.InspectionEntity;
import com.uzenjitrust.build.domain.InspectionStatus;
import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.build.repo.InspectionRepository;
import com.uzenjitrust.build.repo.MilestoneRepository;
import com.uzenjitrust.build.repo.ProjectRepository;
import com.uzenjitrust.common.error.NotFoundException;
import com.uzenjitrust.common.security.AuthorizationService;
import com.uzenjitrust.ledger.service.LedgerPostingService;
import com.uzenjitrust.ledger.service.LedgerTemplateService;
import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.orchestrator.DisbursementOrchestrator;
import com.uzenjitrust.ops.repo.EscrowRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InspectionOrchestrator {

    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final InspectionRepository inspectionRepository;
    private final EscrowRepository escrowRepository;
    private final AuthorizationService authorizationService;
    private final LedgerTemplateService ledgerTemplateService;
    private final LedgerPostingService ledgerPostingService;
    private final DisbursementOrchestrator disbursementOrchestrator;
    private final ObjectMapper objectMapper;

    public InspectionOrchestrator(ProjectRepository projectRepository,
                                  MilestoneRepository milestoneRepository,
                                  InspectionRepository inspectionRepository,
                                  EscrowRepository escrowRepository,
                                  AuthorizationService authorizationService,
                                  LedgerTemplateService ledgerTemplateService,
                                  LedgerPostingService ledgerPostingService,
                                  DisbursementOrchestrator disbursementOrchestrator,
                                  ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.inspectionRepository = inspectionRepository;
        this.escrowRepository = escrowRepository;
        this.authorizationService = authorizationService;
        this.ledgerTemplateService = ledgerTemplateService;
        this.ledgerPostingService = ledgerPostingService;
        this.disbursementOrchestrator = disbursementOrchestrator;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InspectionEntity schedule(ScheduleInspectionRequest request) {
        ProjectEntity project = projectRepository.findById(request.projectId())
                .orElseThrow(() -> new NotFoundException("Project not found"));
        var actor = authorizationService.requireInspector(project.getInspectorUserId());

        MilestoneEntity milestone = milestoneRepository.findById(request.milestoneId())
                .orElseThrow(() -> new NotFoundException("Milestone not found"));

        InspectionEntity inspection = new InspectionEntity();
        inspection.setProject(project);
        inspection.setMilestone(milestone);
        inspection.setInspectorUserId(actor.userId());
        inspection.setStatus(InspectionStatus.SCHEDULED);
        inspection.setScheduledAt(request.scheduledAt());
        inspection.setFeeAmount(request.feeAmount());
        return inspectionRepository.save(inspection);
    }

    @Transactional
    public InspectionEntity complete(java.util.UUID inspectionId, CompleteInspectionRequest request) {
        InspectionEntity inspection = inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new NotFoundException("Inspection not found"));
        var actor = authorizationService.requireInspector(inspection.getInspectorUserId());

        inspection.setStatus(InspectionStatus.COMPLETED);
        inspection.setCompletedAt(java.time.Instant.now());
        try {
            inspection.setReport(objectMapper.readTree(request.reportJson()));
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid reportJson", ex);
        }

        MilestoneEntity milestone = inspection.getMilestone();
        if (milestone != null) {
            milestone.setStatus(MilestoneStatus.INSPECTED);
        }

        EscrowEntity escrow = escrowRepository.findById(inspection.getProject().getEscrowId())
                .orElseThrow(() -> new NotFoundException("Project escrow not found"));

        if (inspection.getFeeAmount() != null && inspection.getFeeAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
            ledgerPostingService.post(ledgerTemplateService.inspectionFeeAuthorized(
                    inspection.getId(),
                    actor.userId(),
                    inspection.getFeeAmount(),
                    "TZS",
                    "INSPECTION_AUTH:" + inspection.getId()
            ));

            disbursementOrchestrator.createDisbursementAndQueuePayout(
                    "INSPECTION_FEE:" + inspection.getId(),
                    "OUTBOX:INSPECTION_FEE:" + inspection.getId(),
                    escrow,
                    milestone == null ? null : milestone.getId(),
                    "INSPECTOR",
                    inspection.getInspectorUserId(),
                    inspection.getFeeAmount(),
                    "TZS"
            );
        }

        return inspection;
    }
}
