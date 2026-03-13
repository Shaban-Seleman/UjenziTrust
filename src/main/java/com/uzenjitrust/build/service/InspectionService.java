package com.uzenjitrust.build.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.uzenjitrust.build.api.InspectionResponse;
import com.uzenjitrust.build.domain.InspectionEntity;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.build.repo.InspectionRepository;
import com.uzenjitrust.build.repo.MilestoneRepository;
import com.uzenjitrust.common.error.ForbiddenException;
import com.uzenjitrust.common.error.NotFoundException;
import com.uzenjitrust.common.security.ActorPrincipal;
import com.uzenjitrust.common.security.ActorProvider;
import com.uzenjitrust.common.security.AppRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InspectionService {

    private final InspectionRepository inspectionRepository;
    private final MilestoneRepository milestoneRepository;
    private final ActorProvider actorProvider;

    public InspectionService(InspectionRepository inspectionRepository,
                             MilestoneRepository milestoneRepository,
                             ActorProvider actorProvider) {
        this.inspectionRepository = inspectionRepository;
        this.milestoneRepository = milestoneRepository;
        this.actorProvider = actorProvider;
    }

    @Transactional(readOnly = true)
    public List<InspectionResponse> listByMilestone(UUID milestoneId) {
        var milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new NotFoundException("Milestone not found"));
        requireProjectAccess(milestone.getProject());
        return inspectionRepository.findByMilestone_IdOrderByCreatedAtDesc(milestoneId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InspectionResponse getVisibleById(UUID inspectionId) {
        InspectionEntity inspection = inspectionRepository.findInspectionEntityById(inspectionId)
                .orElseThrow(() -> new NotFoundException("Inspection not found"));
        requireProjectAccess(inspection.getProject());
        return toResponse(inspection);
    }

    @Transactional(readOnly = true)
    public void applyLatestInspectionSummary(com.uzenjitrust.build.domain.MilestoneEntity milestone) {
        inspectionRepository.findTopByMilestone_IdOrderByCreatedAtDesc(milestone.getId())
                .ifPresentOrElse(inspection -> {
                    milestone.setInspectionId(inspection.getId());
                    milestone.setInspectionStatus(inspection.getStatus().name());
                    milestone.setInspectionResult(resolveResult(inspection.getReport()));
                    milestone.setInspectionCompletedAt(inspection.getCompletedAt());
                }, () -> {
                    milestone.setInspectionId(null);
                    milestone.setInspectionStatus(null);
                    milestone.setInspectionResult(null);
                    milestone.setInspectionCompletedAt(null);
                });
    }

    private InspectionResponse toResponse(InspectionEntity inspection) {
        return new InspectionResponse(
                inspection.getId(),
                inspection.getMilestoneId(),
                inspection.getScheduledAt(),
                inspection.getInspectorUserId(),
                inspection.getStatus().name(),
                resolveResult(inspection.getReport()),
                inspection.getReport(),
                inspection.getCompletedAt(),
                inspection.getCreatedAt()
        );
    }

    private String resolveResult(JsonNode report) {
        if (report == null) {
            return "PENDING";
        }
        JsonNode resultNode = report.get("result");
        if (resultNode == null || resultNode.isNull()) {
            return "PENDING";
        }
        String value = resultNode.asText("").trim().toUpperCase();
        if ("PASS".equals(value) || "FAIL".equals(value)) {
            return value;
        }
        return "PENDING";
    }

    private void requireProjectAccess(ProjectEntity project) {
        ActorPrincipal actor = actorProvider.requireActor();
        if (actor.roles().contains(AppRole.ADMIN)) {
            return;
        }

        boolean canAccess = actor.userId().equals(project.getOwnerUserId())
                || actor.userId().equals(project.getContractorUserId())
                || actor.userId().equals(project.getInspectorUserId());
        if (!canAccess) {
            throw new ForbiddenException("Project access denied");
        }
    }
}
