package com.uzenjitrust.build.service;

import com.uzenjitrust.build.api.AssignParticipantsRequest;
import com.uzenjitrust.build.api.CreateProjectRequest;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.build.domain.ProjectStatus;
import com.uzenjitrust.build.repo.ProjectRepository;
import com.uzenjitrust.common.error.BadRequestException;
import com.uzenjitrust.common.error.NotFoundException;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.common.security.AuthorizationService;
import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.service.EscrowService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class BuildProjectService {

    private final ProjectRepository projectRepository;
    private final AuthorizationService authorizationService;
    private final EscrowService escrowService;

    public BuildProjectService(ProjectRepository projectRepository,
                               AuthorizationService authorizationService,
                               EscrowService escrowService) {
        this.projectRepository = projectRepository;
        this.authorizationService = authorizationService;
        this.escrowService = escrowService;
    }

    @Transactional
    public ProjectEntity createDraft(CreateProjectRequest request) {
        var actor = authorizationService.requireRole(AppRole.OWNER);
        if (request.title() == null || request.title().isBlank()) {
            throw new BadRequestException("title is required");
        }

        ProjectEntity project = new ProjectEntity();
        project.setOwnerUserId(actor.userId());
        project.setTitle(request.title());
        project.setDescription(request.description());
        project.setStatus(ProjectStatus.DRAFT);
        project.setRetentionRate(request.retentionRate() == null ? new BigDecimal("10.00") : request.retentionRate());

        ProjectEntity saved = projectRepository.save(project);
        EscrowEntity escrow = escrowService.createEscrowIdempotent(
                "CONSTRUCTION_PROJECT:" + saved.getId(),
                "CONSTRUCTION_PROJECT",
                request.totalBudget() == null ? BigDecimal.ZERO : request.totalBudget(),
                request.currency() == null ? "TZS" : request.currency(),
                actor.userId(),
                null
        );
        saved.setEscrowId(escrow.getId());
        return saved;
    }

    @Transactional
    public ProjectEntity assignParticipants(UUID projectId, AssignParticipantsRequest request) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        authorizationService.requireOwner(project.getOwnerUserId());
        project.setContractorUserId(request.contractorUserId());
        project.setInspectorUserId(request.inspectorUserId());
        return project;
    }

    @Transactional
    public ProjectEntity activate(UUID projectId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        authorizationService.requireOwner(project.getOwnerUserId());

        if (project.getContractorUserId() == null || project.getInspectorUserId() == null) {
            throw new BadRequestException("Contractor and inspector must be assigned");
        }
        if (project.getEscrowId() == null) {
            throw new BadRequestException("Project escrow is required");
        }

        project.setStatus(ProjectStatus.ACTIVE);
        return project;
    }
}
