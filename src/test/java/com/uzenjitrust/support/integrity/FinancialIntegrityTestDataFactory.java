package com.uzenjitrust.support.integrity;

import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.build.domain.ProjectStatus;
import com.uzenjitrust.build.repo.MilestoneRepository;
import com.uzenjitrust.build.repo.ProjectRepository;
import com.uzenjitrust.market.domain.OfferEntity;
import com.uzenjitrust.market.domain.OfferStatus;
import com.uzenjitrust.market.domain.PropertyEntity;
import com.uzenjitrust.market.domain.PropertyStatus;
import com.uzenjitrust.market.repo.OfferRepository;
import com.uzenjitrust.market.repo.PropertyRepository;
import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.service.EscrowService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Component
public class FinancialIntegrityTestDataFactory {

    private final PropertyRepository propertyRepository;
    private final OfferRepository offerRepository;
    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final EscrowService escrowService;

    public FinancialIntegrityTestDataFactory(PropertyRepository propertyRepository,
                                             OfferRepository offerRepository,
                                             ProjectRepository projectRepository,
                                             MilestoneRepository milestoneRepository,
                                             EscrowService escrowService) {
        this.propertyRepository = propertyRepository;
        this.offerRepository = offerRepository;
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.escrowService = escrowService;
    }

    public PropertyEntity property(UUID ownerUserId, String title, BigDecimal askingPrice) {
        PropertyEntity property = new PropertyEntity();
        property.setOwnerUserId(ownerUserId);
        property.setTitle(title);
        property.setDescription(title + " description");
        property.setLocation("Dar es Salaam");
        property.setAskingPrice(askingPrice);
        property.setCurrency("TZS");
        property.setStatus(PropertyStatus.PUBLISHED);
        return propertyRepository.save(property);
    }

    public OfferEntity offer(PropertyEntity property, UUID buyerUserId, BigDecimal amount) {
        OfferEntity offer = new OfferEntity();
        offer.setProperty(property);
        offer.setBuyerUserId(buyerUserId);
        offer.setSellerUserId(property.getOwnerUserId());
        offer.setAmount(amount);
        offer.setCurrency("TZS");
        offer.setStatus(OfferStatus.SUBMITTED);
        offer.setExpiresAt(Instant.now().plus(3, ChronoUnit.DAYS));
        return offerRepository.save(offer);
    }

    public ProjectEntity projectWithEscrow(UUID ownerUserId,
                                           UUID contractorUserId,
                                           UUID inspectorUserId,
                                           String keySuffix,
                                           BigDecimal escrowAmount) {
        EscrowEntity escrow = escrowService.createEscrowIdempotent(
                "CONSTRUCTION_PROJECT:" + keySuffix,
                "CONSTRUCTION_PROJECT",
                escrowAmount,
                "TZS",
                ownerUserId,
                contractorUserId
        );

        ProjectEntity project = new ProjectEntity();
        project.setOwnerUserId(ownerUserId);
        project.setContractorUserId(contractorUserId);
        project.setInspectorUserId(inspectorUserId);
        project.setEscrowId(escrow.getId());
        project.setTitle("Project " + keySuffix);
        project.setDescription("Test project " + keySuffix);
        project.setStatus(ProjectStatus.ACTIVE);
        project.setRetentionRate(new BigDecimal("10.00"));
        return projectRepository.save(project);
    }

    public MilestoneEntity milestone(ProjectEntity project,
                                     int sequence,
                                     BigDecimal amount,
                                     BigDecimal retention,
                                     MilestoneStatus status) {
        MilestoneEntity milestone = new MilestoneEntity();
        milestone.setProject(project);
        milestone.setName("Milestone " + sequence);
        milestone.setDescription("Milestone " + sequence + " description");
        milestone.setSequenceNo(sequence);
        milestone.setAmount(amount);
        milestone.setRetentionAmount(retention);
        milestone.setStatus(status);
        return milestoneRepository.save(milestone);
    }
}
