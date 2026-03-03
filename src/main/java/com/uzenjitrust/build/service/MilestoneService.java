package com.uzenjitrust.build.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uzenjitrust.build.api.CreateMilestoneRequest;
import com.uzenjitrust.build.api.SubmitMilestoneEvidenceRequest;
import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import com.uzenjitrust.build.domain.MilestoneSubmissionEntity;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.build.domain.SubmissionStatus;
import com.uzenjitrust.build.repo.MilestoneRepository;
import com.uzenjitrust.build.repo.MilestoneSubmissionRepository;
import com.uzenjitrust.build.repo.ProjectRepository;
import com.uzenjitrust.common.error.ForbiddenException;
import com.uzenjitrust.common.error.NotFoundException;
import com.uzenjitrust.common.security.ActorPrincipal;
import com.uzenjitrust.common.security.ActorProvider;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.common.security.AuthorizationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class MilestoneService {

    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final MilestoneSubmissionRepository submissionRepository;
    private final AuthorizationService authorizationService;
    private final ActorProvider actorProvider;
    private final ObjectMapper objectMapper;

    public MilestoneService(ProjectRepository projectRepository,
                            MilestoneRepository milestoneRepository,
                            MilestoneSubmissionRepository submissionRepository,
                            AuthorizationService authorizationService,
                            ActorProvider actorProvider,
                            ObjectMapper objectMapper) {
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.submissionRepository = submissionRepository;
        this.authorizationService = authorizationService;
        this.actorProvider = actorProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public MilestoneEntity create(UUID projectId, CreateMilestoneRequest request) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        authorizationService.requireOwner(project.getOwnerUserId());

        MilestoneEntity milestone = new MilestoneEntity();
        milestone.setProject(project);
        milestone.setName(request.name());
        milestone.setDescription(request.description());
        milestone.setSequenceNo(request.sequenceNo());
        milestone.setAmount(request.amount());

        BigDecimal retentionAmount = request.retentionAmount();
        if (retentionAmount == null) {
            retentionAmount = request.amount()
                    .multiply(project.getRetentionRate())
                    .divide(new BigDecimal("100"));
        }
        milestone.setRetentionAmount(retentionAmount);
        milestone.setStatus(MilestoneStatus.PLANNED);
        milestone.setDueDate(request.dueDate());
        return milestoneRepository.save(milestone);
    }

    @Transactional(readOnly = true)
    public List<MilestoneEntity> listByProject(UUID projectId) {
        ProjectEntity project = projectRepository.findById(projectId)
                .orElseThrow(() -> new NotFoundException("Project not found"));
        requireProjectAccess(project);
        return milestoneRepository.findByProject_IdOrderBySequenceNoAsc(projectId);
    }

    @Transactional
    public MilestoneSubmissionEntity submitEvidence(UUID milestoneId, SubmitMilestoneEvidenceRequest request) {
        MilestoneEntity milestone = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new NotFoundException("Milestone not found"));
        ProjectEntity project = milestone.getProject();
        var actor = authorizationService.requireContractor(project.getContractorUserId());

        milestone.setStatus(MilestoneStatus.SUBMITTED);

        MilestoneSubmissionEntity submission = new MilestoneSubmissionEntity();
        submission.setMilestone(milestone);
        submission.setSubmittedBy(actor.userId());
        submission.setEvidence(objectMapper.valueToTree(request.evidence()));
        submission.setNotes(request.notes());
        submission.setStatus(SubmissionStatus.SUBMITTED);
        return submissionRepository.save(submission);
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
