package com.uzenjitrust.build.service;

import com.uzenjitrust.build.api.AssignParticipantsRequest;
import com.uzenjitrust.build.api.CreateProjectRequest;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.build.domain.ProjectStatus;
import com.uzenjitrust.build.repo.ProjectRepository;
import com.uzenjitrust.common.error.BadRequestException;
import com.uzenjitrust.common.error.ForbiddenException;
import com.uzenjitrust.common.error.NotFoundException;
import com.uzenjitrust.common.security.ActorPrincipal;
import com.uzenjitrust.common.security.ActorProvider;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.common.security.AuthorizationService;
import com.uzenjitrust.ops.service.OperatorAuditService;
import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.service.EscrowService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class BuildProjectService {

    private final ProjectRepository projectRepository;
    private final AuthorizationService authorizationService;
    private final EscrowService escrowService;
    private final ActorProvider actorProvider;
    private final OperatorAuditService operatorAuditService;

    public BuildProjectService(ProjectRepository projectRepository,
                               AuthorizationService authorizationService,
                               EscrowService escrowService,
                               ActorProvider actorProvider,
                               OperatorAuditService operatorAuditService) {
        this.projectRepository = projectRepository;
        this.authorizationService = authorizationService;
        this.escrowService = escrowService;
        this.actorProvider = actorProvider;
        this.operatorAuditService = operatorAuditService;
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
        try {
            ProjectEntity project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new NotFoundException("Project not found"));
            authorizationService.requireOwner(project.getOwnerUserId());
            project.setContractorUserId(request.contractorUserId());
            project.setInspectorUserId(request.inspectorUserId());
            operatorAuditService.recordSuccess(
                    "PROJECT_ASSIGN_PARTICIPANTS",
                    "PROJECT",
                    projectId.toString(),
                    "Assigned contractor and inspector",
                    java.util.Map.of(
                            "contractorUserId", request.contractorUserId(),
                            "inspectorUserId", request.inspectorUserId()
                    )
            );
            return project;
        } catch (ForbiddenException ex) {
            operatorAuditService.recordForbidden("PROJECT_ASSIGN_PARTICIPANTS", "PROJECT", projectId.toString(), "Project assignment forbidden", java.util.Map.of(
                    "contractorUserId", request.contractorUserId(),
                    "inspectorUserId", request.inspectorUserId()
            ), ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            operatorAuditService.recordFailure("PROJECT_ASSIGN_PARTICIPANTS", "PROJECT", projectId.toString(), "Project assignment failed", java.util.Map.of(
                    "contractorUserId", request.contractorUserId(),
                    "inspectorUserId", request.inspectorUserId()
            ), ex.getMessage());
            throw ex;
        }
    }

    @Transactional
    public ProjectEntity activate(UUID projectId) {
        try {
            ProjectEntity project = projectRepository.findById(projectId)
                    .orElseThrow(() -> new NotFoundException("Project not found"));
            authorizationService.requireOwner(project.getOwnerUserId());

            ProjectStatus previousStatus = project.getStatus();
            if (project.getContractorUserId() == null || project.getInspectorUserId() == null) {
                throw new BadRequestException("Contractor and inspector must be assigned");
            }
            if (project.getEscrowId() == null) {
                throw new BadRequestException("Project escrow is required");
            }

            project.setStatus(ProjectStatus.ACTIVE);
            operatorAuditService.recordSuccess(
                    "PROJECT_ACTIVATED",
                    "PROJECT",
                    projectId.toString(),
                    "Project activated",
                    java.util.Map.of("fromStatus", previousStatus.name(), "toStatus", ProjectStatus.ACTIVE.name())
            );
            return project;
        } catch (ForbiddenException ex) {
            operatorAuditService.recordForbidden("PROJECT_ACTIVATED", "PROJECT", projectId.toString(), "Project activation forbidden", null, ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            operatorAuditService.recordFailure("PROJECT_ACTIVATED", "PROJECT", projectId.toString(), "Project activation failed", null, ex.getMessage());
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Page<ProjectEntity> listVisible(int page, int size, String sortBy, Sort.Direction direction) {
        ActorPrincipal actor = actorProvider.requireActor();
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        if (actor.roles().contains(AppRole.ADMIN)) {
            return projectRepository.findAll(pageable);
        }
        if (!actor.roles().contains(AppRole.OWNER)
                && !actor.roles().contains(AppRole.CONTRACTOR)
                && !actor.roles().contains(AppRole.INSPECTOR)) {
            throw new ForbiddenException("Insufficient role");
        }
        return projectRepository.findByOwnerUserIdOrContractorUserIdOrInspectorUserId(
                actor.userId(),
                actor.userId(),
                actor.userId(),
                pageable
        );
    }

    @Transactional(readOnly = true)
    public ProjectEntity getVisibleById(UUID projectId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        ActorPrincipal actor = actorProvider.requireActor();
        if (actor.roles().contains(AppRole.ADMIN)) {
            return project;
        }
        boolean canAccess = actor.userId().equals(project.getOwnerUserId())
                || actor.userId().equals(project.getContractorUserId())
                || actor.userId().equals(project.getInspectorUserId());
        if (!canAccess) {
            throw new ForbiddenException("Project access denied");
        }
        return project;
    }
}
