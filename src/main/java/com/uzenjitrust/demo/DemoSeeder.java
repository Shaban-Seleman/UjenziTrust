package com.uzenjitrust.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.uzenjitrust.build.api.ApproveMilestoneMultiRequest;
import com.uzenjitrust.build.api.AssignParticipantsRequest;
import com.uzenjitrust.build.api.CompleteInspectionRequest;
import com.uzenjitrust.build.api.CreateMilestoneRequest;
import com.uzenjitrust.build.api.CreateProjectRequest;
import com.uzenjitrust.build.api.ScheduleInspectionRequest;
import com.uzenjitrust.build.api.SubmitMilestoneEvidenceRequest;
import com.uzenjitrust.build.domain.MilestoneEntity;
import com.uzenjitrust.build.domain.MilestoneStatus;
import com.uzenjitrust.build.domain.ProjectEntity;
import com.uzenjitrust.build.repo.MilestoneRepository;
import com.uzenjitrust.build.repo.ProjectRepository;
import com.uzenjitrust.build.service.BuildProjectService;
import com.uzenjitrust.build.service.MilestoneService;
import com.uzenjitrust.build.orchestrator.InspectionOrchestrator;
import com.uzenjitrust.build.orchestrator.MilestoneOrchestrator;
import com.uzenjitrust.build.orchestrator.MultiPartyMilestoneOrchestrator;
import com.uzenjitrust.common.security.ActorPrincipal;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.ledger.service.LedgerPostingService;
import com.uzenjitrust.ledger.service.LedgerTemplateService;
import com.uzenjitrust.market.api.AcceptOfferRequest;
import com.uzenjitrust.market.api.CounterOfferRequest;
import com.uzenjitrust.market.api.CreatePropertyRequest;
import com.uzenjitrust.market.api.SubmitOfferRequest;
import com.uzenjitrust.market.domain.OfferEntity;
import com.uzenjitrust.market.domain.PropertyEntity;
import com.uzenjitrust.market.domain.PropertyReservationEntity;
import com.uzenjitrust.market.repo.OfferRepository;
import com.uzenjitrust.market.repo.PropertyRepository;
import com.uzenjitrust.market.repo.PropertyReservationRepository;
import com.uzenjitrust.market.service.OfferService;
import com.uzenjitrust.market.service.PropertyService;
import com.uzenjitrust.ops.api.SettlementWebhookRequest;
import com.uzenjitrust.ops.domain.DisbursementOrderEntity;
import com.uzenjitrust.ops.domain.DisbursementStatus;
import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.domain.EscrowStatus;
import com.uzenjitrust.ops.repo.DisbursementOrderRepository;
import com.uzenjitrust.ops.repo.EscrowRepository;
import com.uzenjitrust.ops.service.EscrowService;
import com.uzenjitrust.ops.service.WebhookService;
import com.uzenjitrust.users.domain.UserEntity;
import com.uzenjitrust.users.domain.UserRoleEntity;
import com.uzenjitrust.users.domain.UserRoleId;
import com.uzenjitrust.users.domain.UserStatus;
import com.uzenjitrust.users.repo.UserRepository;
import com.uzenjitrust.users.repo.UserRoleRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class DemoSeeder {

    private static final String DEMO_PASSWORD = "Demo123!";
    private static final String CURRENCY = "TZS";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final PropertyService propertyService;
    private final OfferService offerService;
    private final BuildProjectService buildProjectService;
    private final MilestoneService milestoneService;
    private final MilestoneOrchestrator milestoneOrchestrator;
    private final MultiPartyMilestoneOrchestrator multiPartyMilestoneOrchestrator;
    private final InspectionOrchestrator inspectionOrchestrator;
    private final EscrowService escrowService;
    private final WebhookService webhookService;
    private final LedgerPostingService ledgerPostingService;
    private final LedgerTemplateService ledgerTemplateService;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PropertyRepository propertyRepository;
    private final OfferRepository offerRepository;
    private final PropertyReservationRepository reservationRepository;
    private final EscrowRepository escrowRepository;
    private final ProjectRepository projectRepository;
    private final MilestoneRepository milestoneRepository;
    private final DisbursementOrderRepository disbursementRepository;
    private final String webhookSecret;

    public DemoSeeder(JdbcTemplate jdbcTemplate,
                      PasswordEncoder passwordEncoder,
                      ObjectMapper objectMapper,
                      PropertyService propertyService,
                      OfferService offerService,
                      BuildProjectService buildProjectService,
                      MilestoneService milestoneService,
                      MilestoneOrchestrator milestoneOrchestrator,
                      MultiPartyMilestoneOrchestrator multiPartyMilestoneOrchestrator,
                      InspectionOrchestrator inspectionOrchestrator,
                      EscrowService escrowService,
                      WebhookService webhookService,
                      LedgerPostingService ledgerPostingService,
                      LedgerTemplateService ledgerTemplateService,
                      UserRepository userRepository,
                      UserRoleRepository userRoleRepository,
                      PropertyRepository propertyRepository,
                      OfferRepository offerRepository,
                      PropertyReservationRepository reservationRepository,
                      EscrowRepository escrowRepository,
                      ProjectRepository projectRepository,
                      MilestoneRepository milestoneRepository,
                      DisbursementOrderRepository disbursementRepository,
                      @Value("${app.ops.webhook-secret}") String webhookSecret) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
        this.propertyService = propertyService;
        this.offerService = offerService;
        this.buildProjectService = buildProjectService;
        this.milestoneService = milestoneService;
        this.milestoneOrchestrator = milestoneOrchestrator;
        this.multiPartyMilestoneOrchestrator = multiPartyMilestoneOrchestrator;
        this.inspectionOrchestrator = inspectionOrchestrator;
        this.escrowService = escrowService;
        this.webhookService = webhookService;
        this.ledgerPostingService = ledgerPostingService;
        this.ledgerTemplateService = ledgerTemplateService;
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.propertyRepository = propertyRepository;
        this.offerRepository = offerRepository;
        this.reservationRepository = reservationRepository;
        this.escrowRepository = escrowRepository;
        this.projectRepository = projectRepository;
        this.milestoneRepository = milestoneRepository;
        this.disbursementRepository = disbursementRepository;
        this.webhookSecret = webhookSecret;
    }

    @Transactional
    public void resetAll() {
        List<String> tables = new ArrayList<>(jdbcTemplate.queryForList("""
                select schemaname || '.' || tablename
                from pg_tables
                where schemaname in ('market', 'ops', 'build', 'ledger')
                order by schemaname, tablename
                """, String.class));
        tables.add("users.sessions");
        tables.add("users.user_roles");
        tables.add("users.users");

        jdbcTemplate.execute("TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE");
        reseedLedgerAccounts();
    }

    @Transactional
    public DemoSeedSummary seedInvestorScenario() {
        Instant now = Instant.now();
        Map<String, DemoUser> users = seedUsers();

        List<PropertyEntity> properties = seedProperties(users);
        List<OfferEntity> offers = new ArrayList<>();
        List<PropertyReservationEntity> acceptedReservations = new ArrayList<>();
        List<EscrowEntity> acceptedEscrows = new ArrayList<>();

        for (int i = 0; i < properties.size(); i++) {
            PropertyEntity property = properties.get(i);
            OfferFlowResult flow = createOfferFlow(property, i, users);
            offers.addAll(flow.offers());
            if (flow.reservation() != null) {
                acceptedReservations.add(flow.reservation());
            }
            if (flow.purchaseEscrow() != null) {
                acceptedEscrows.add(flow.purchaseEscrow());
            }
        }

        expireDemoOffersAndReservations(offers, acceptedReservations, now);

        List<EscrowEntity> extraEscrows = seedEscrowScenarios(users);
        List<EscrowEntity> allEscrows = new ArrayList<>(acceptedEscrows);
        allEscrows.addAll(extraEscrows);

        List<ProjectEntity> projects = seedProjects(users);
        MilestoneScenarioResult milestoneResult = seedMilestones(projects, users, now);

        allEscrows.addAll(projects.stream()
                .map(project -> escrowRepository.findById(project.getEscrowId()).orElseThrow())
                .toList());
        normalizeEscrowStatuses(allEscrows, users.get("seller"), now);

        long retentionReadyCount = milestoneRepository.findAll().stream()
                .filter(milestone -> milestone.getStatus() == MilestoneStatus.PAID)
                .filter(milestone -> milestone.getRetentionReleaseAt() != null)
                .filter(milestone -> milestone.getRetentionReleasedAt() == null)
                .filter(milestone -> !milestone.getRetentionReleaseAt().isAfter(now))
                .count();

        return new DemoSeedSummary(
                Math.toIntExact(userRepository.count()),
                Math.toIntExact(propertyRepository.count()),
                Math.toIntExact(offerRepository.count()),
                Math.toIntExact(escrowRepository.count()),
                Math.toIntExact(projectRepository.count()),
                Math.toIntExact(milestoneRepository.count()),
                Math.toIntExact(disbursementRepository.count()),
                milestoneResult.settledDisbursements(),
                Math.toIntExact(retentionReadyCount)
        );
    }

    private Map<String, DemoUser> seedUsers() {
        LinkedHashMap<String, DemoUser> users = new LinkedHashMap<>();
        users.put("admin", createUser("demo-admin@nyumbatrust.local", "255700000001", Set.of(AppRole.ADMIN)));
        users.put("seller", createUser("demo-seller@nyumbatrust.local", "255700000002", Set.of(AppRole.SELLER)));
        users.put("owner", createUser("demo-owner@nyumbatrust.local", "255700000003", Set.of(AppRole.OWNER, AppRole.SELLER)));
        users.put("contractor", createUser("demo-contractor@nyumbatrust.local", "255700000004", Set.of(AppRole.CONTRACTOR)));
        users.put("inspector", createUser("demo-inspector@nyumbatrust.local", "255700000005", Set.of(AppRole.INSPECTOR)));
        for (int i = 1; i <= 5; i++) {
            users.put("buyer" + i, createUser(
                    "demo-buyer-" + i + "@nyumbatrust.local",
                    "25570000010" + i,
                    Set.of(AppRole.BUYER)
            ));
        }
        return users;
    }

    private DemoUser createUser(String email, String phone, Set<AppRole> roles) {
        UserEntity user = new UserEntity();
        user.setId(stableUuid("demo-user:" + email));
        user.setEmail(email);
        user.setPhone(phone);
        user.setStatus(UserStatus.ACTIVE);
        user.setPasswordHash(passwordEncoder.encode(DEMO_PASSWORD));
        userRepository.save(user);

        for (AppRole role : roles) {
            UserRoleEntity userRole = new UserRoleEntity();
            userRole.setId(new UserRoleId(user.getId(), role.name()));
            userRoleRepository.save(userRole);
        }

        return new DemoUser(user.getId(), email, roles);
    }

    private List<PropertyEntity> seedProperties(Map<String, DemoUser> users) {
        String[] regions = {"Dar es Salaam", "Arusha", "Dodoma", "Mwanza", "Zanzibar", "Morogoro"};
        String[] types = {"Apartment", "Villa", "Townhouse", "Plot"};
        List<PropertyEntity> properties = new ArrayList<>();

        for (int i = 0; i < 24; i++) {
            DemoUser owner = (i % 3 == 0) ? users.get("owner") : users.get("seller");
            int index = i;
            PropertyEntity property = runAs(owner, () -> {
                CreatePropertyRequest request = new CreatePropertyRequest(
                        types[index % types.length] + " " + (index + 1),
                        "Demo-ready " + types[index % types.length] + " investment property",
                        regions[index % regions.length] + ", Tanzania",
                        new BigDecimal("85000000").add(new BigDecimal(index * 7500000L)),
                        CURRENCY
                );
                PropertyEntity created = propertyService.createDraft(request);
                return propertyService.publish(created.getId());
            });
            properties.add(property);
        }

        return properties;
    }

    private OfferFlowResult createOfferFlow(PropertyEntity property, int propertyIndex, Map<String, DemoUser> users) {
        List<DemoUser> buyers = List.of(
                users.get("buyer1"),
                users.get("buyer2"),
                users.get("buyer3"),
                users.get("buyer4"),
                users.get("buyer5")
        );
        List<OfferEntity> offers = new ArrayList<>();
        List<DemoUser> offerBuyers = new ArrayList<>();
        BigDecimal base = property.getAskingPrice();

        for (int i = 0; i < 3; i++) {
            DemoUser buyer = buyers.get((propertyIndex + i) % buyers.size());
            final int offerNo = i + 1;
            OfferEntity offer = runAs(buyer, () -> offerService.submit(
                    property.getId(),
                    new SubmitOfferRequest(
                            base.subtract(new BigDecimal(offerNo * 1500000L)),
                            CURRENCY,
                            "Demo investor offer " + offerNo
                    )
            ));
            offers.add(offer);
            offerBuyers.add(buyer);
        }

        DemoUser seller = userForProperty(property, users);
        PropertyReservationEntity reservation = null;
        EscrowEntity purchaseEscrow = null;

        switch (propertyIndex % 6) {
            case 0 -> {
                OfferEntity countered = runAs(seller, () -> offerService.counter(
                        offers.get(0).getId(),
                        new CounterOfferRequest(base.subtract(new BigDecimal("500000")), "Countered for demo")
                ));
                offers.set(0, countered);
            }
            case 1 -> {
                OfferService.AcceptOfferResult accepted = runAs(seller, () -> offerService.accept(
                        offers.get(0).getId(),
                        new AcceptOfferRequest("ACCEPT:" + property.getId(), "Accepted for demo")
                ));
                offers.set(0, accepted.offer());
                reservation = accepted.reservation();
                purchaseEscrow = escrowRepository.findByBusinessKey("PROPERTY_PURCHASE:" + reservation.getId()).orElseThrow();
            }
            case 2 -> {
                OfferEntity rejected = runAs(seller, () -> offerService.reject(offers.get(0).getId(), "Rejected for demo"));
                OfferEntity withdrawn = runAs(offerBuyers.get(1), () -> offerService.withdraw(offers.get(1).getId(), "Buyer withdrew"));
                offers.set(0, rejected);
                offers.set(1, withdrawn);
            }
            case 3 -> {
                OfferEntity countered = runAs(seller, () -> offerService.counter(
                        offers.get(0).getId(),
                        new CounterOfferRequest(base.subtract(new BigDecimal("750000")), "Countered then expired")
                ));
                offers.set(0, countered);
            }
            case 4 -> {
                OfferService.AcceptOfferResult accepted = runAs(seller, () -> offerService.accept(
                        offers.get(1).getId(),
                        new AcceptOfferRequest("ACCEPT:" + property.getId(), "Accepted then cancelled")
                ));
                offers.set(1, accepted.offer());
                reservation = runAs(seller, () -> offerService.cancelReservation(accepted.reservation().getId(), "Seller cancelled demo hold"));
                purchaseEscrow = escrowRepository.findByBusinessKey("PROPERTY_PURCHASE:" + accepted.reservation().getId()).orElseThrow();
            }
            case 5 -> {
                OfferService.AcceptOfferResult accepted = runAs(seller, () -> offerService.accept(
                        offers.get(2).getId(),
                        new AcceptOfferRequest("ACCEPT:" + property.getId(), "Accepted and will expire")
                ));
                offers.set(2, accepted.offer());
                accepted.reservation().setReservedUntil(Instant.now().minus(2, ChronoUnit.DAYS));
                reservation = accepted.reservation();
                purchaseEscrow = escrowRepository.findByBusinessKey("PROPERTY_PURCHASE:" + accepted.reservation().getId()).orElseThrow();
            }
            default -> {
            }
        }

        return new OfferFlowResult(offers, reservation, purchaseEscrow);
    }

    private void expireDemoOffersAndReservations(List<OfferEntity> offers,
                                                 List<PropertyReservationEntity> reservations,
                                                 Instant now) {
        for (int i = 0; i < offers.size(); i++) {
            OfferEntity offer = offers.get(i);
            if (i % 11 == 0 && (offer.getStatus().name().equals("SUBMITTED") || offer.getStatus().name().equals("COUNTERED"))) {
                offer.setExpiresAt(now.minus(1, ChronoUnit.DAYS));
            }
        }
        for (PropertyReservationEntity reservation : reservations) {
            if (reservation != null && reservation.getStatus().name().equals("ACTIVE")) {
                reservation.setReservedUntil(now.minus(3, ChronoUnit.HOURS));
                break;
            }
        }
        offerRepository.expireOffers(now);
        reservationRepository.expireReservations(now);
    }

    private List<EscrowEntity> seedEscrowScenarios(Map<String, DemoUser> users) {
        List<EscrowEntity> escrows = new ArrayList<>();
        DemoUser seller = users.get("seller");
        DemoUser owner = users.get("owner");
        DemoUser buyer1 = users.get("buyer1");
        DemoUser buyer2 = users.get("buyer2");
        DemoUser buyer3 = users.get("buyer3");

        escrows.add(escrowService.createEscrowIdempotent("DEMO:ESCROW:INITIATED", "PROPERTY_PURCHASE", new BigDecimal("120000000"), CURRENCY, buyer1.id(), seller.id()));
        escrows.add(escrowService.createEscrowIdempotent("DEMO:ESCROW:FUNDED", "PROPERTY_PURCHASE", new BigDecimal("130000000"), CURRENCY, buyer2.id(), seller.id()));
        escrows.add(escrowService.createEscrowIdempotent("DEMO:ESCROW:ACTIVE", "PROPERTY_PURCHASE", new BigDecimal("98000000"), CURRENCY, buyer3.id(), seller.id()));
        escrows.add(escrowService.createEscrowIdempotent("DEMO:ESCROW:COMPLETED", "PROPERTY_PURCHASE", new BigDecimal("150000000"), CURRENCY, buyer1.id(), seller.id()));
        escrows.add(escrowService.createEscrowIdempotent("DEMO:ESCROW:CANCELLED", "PROPERTY_PURCHASE", new BigDecimal("110000000"), CURRENCY, buyer2.id(), seller.id()));
        escrows.add(escrowService.createEscrowIdempotent("DEMO:ESCROW:DISPUTED", "CONSTRUCTION_PROJECT", new BigDecimal("210000000"), CURRENCY, owner.id(), seller.id()));

        return escrows;
    }

    private List<ProjectEntity> seedProjects(Map<String, DemoUser> users) {
        List<ProjectEntity> projects = new ArrayList<>();
        DemoUser owner = users.get("owner");

        for (int i = 1; i <= 3; i++) {
            int index = i;
            ProjectEntity project = runAs(owner, () -> {
                ProjectEntity created = buildProjectService.createDraft(new CreateProjectRequest(
                        "Investor Demo Project " + index,
                        "Construction demo project " + index,
                        new BigDecimal("480000000").add(new BigDecimal(index * 25000000L)),
                        CURRENCY,
                        new BigDecimal("10.00")
                ));
                buildProjectService.assignParticipants(created.getId(), new AssignParticipantsRequest(
                        users.get("contractor").id(),
                        users.get("inspector").id()
                ));
                return buildProjectService.activate(created.getId());
            });
            projects.add(project);
        }

        return projects;
    }

    private MilestoneScenarioResult seedMilestones(List<ProjectEntity> projects,
                                                   Map<String, DemoUser> users,
                                                   Instant now) {
        DemoUser owner = users.get("owner");
        DemoUser contractor = users.get("contractor");
        DemoUser inspector = users.get("inspector");
        DemoUser seller = users.get("seller");

        List<MilestoneEntity> milestones = new ArrayList<>();
        int settledDisbursements = 0;

        for (int p = 0; p < projects.size(); p++) {
            ProjectEntity project = projects.get(p);
            for (int m = 1; m <= 4; m++) {
                final int seq = m;
                final int projectNo = p + 1;
                MilestoneEntity milestone = runAs(owner, () -> milestoneService.create(
                        project.getId(),
                        new CreateMilestoneRequest(
                                "Phase " + seq,
                                "Milestone " + seq + " for project " + projectNo,
                                seq,
                                new BigDecimal("100000000").add(new BigDecimal(seq * 12000000L)),
                                new BigDecimal("10000000").add(new BigDecimal(seq * 1000000L)),
                                LocalDate.now().plusDays((long) seq * 7)
                        )
                ));
                milestones.add(milestone);
            }
        }

        // Project 1: paid, partial multi-party, submitted, planned.
        MilestoneEntity p1m1 = milestones.get(0);
        runAs(contractor, () -> milestoneService.submitEvidence(p1m1.getId(), evidence("Concrete poured")));
        runAs(owner, () -> milestoneOrchestrator.approveMilestoneSingle(p1m1.getId(), "DEMO:P1:M1"));
        settledDisbursements += settleAllDisbursementsForMilestone(p1m1, "P1M1");
        p1m1.setRetentionReleaseAt(now.minus(2, ChronoUnit.DAYS));

        MilestoneEntity p1m2 = milestones.get(1);
        runAs(contractor, () -> milestoneService.submitEvidence(p1m2.getId(), evidence("Framing complete")));
        runAs(owner, () -> multiPartyMilestoneOrchestrator.approveMilestoneMulti(
                p1m2.getId(),
                new ApproveMilestoneMultiRequest(
                        "DEMO:P1:M2",
                        List.of(
                                new ApproveMilestoneMultiRequest.Split("CONTRACTOR", contractor.id(), new BigDecimal("70000000"), "DEMO:P1:M2:CTR"),
                                new ApproveMilestoneMultiRequest.Split("SUPPLIER", seller.id(), new BigDecimal("30000000"), "DEMO:P1:M2:SUP"),
                                new ApproveMilestoneMultiRequest.Split("INSPECTOR", inspector.id(), new BigDecimal("12000000"), "DEMO:P1:M2:INSP")
                        )
                )
        ));
        settledDisbursements += settleSomeDisbursementsForMilestone(p1m2, "P1M2", 2);

        MilestoneEntity p1m3 = milestones.get(2);
        runAs(contractor, () -> milestoneService.submitEvidence(p1m3.getId(), evidence("Roofing submitted")));

        // Project 2: inspected, paid, approved pending, planned.
        MilestoneEntity p2m1 = milestones.get(4);
        runAs(contractor, () -> milestoneService.submitEvidence(p2m1.getId(), evidence("Utilities submitted")));
        var inspection = runAs(inspector, () -> inspectionOrchestrator.schedule(new ScheduleInspectionRequest(
                p2m1.getProjectId(),
                p2m1.getId(),
                now.plus(1, ChronoUnit.DAYS),
                BigDecimal.ZERO
        )));
        runAs(inspector, () -> inspectionOrchestrator.complete(inspection.getId(), new CompleteInspectionRequest("{\"result\":\"PASS\"}")));

        MilestoneEntity p2m2 = milestones.get(5);
        runAs(contractor, () -> milestoneService.submitEvidence(p2m2.getId(), evidence("Interiors submitted")));
        runAs(owner, () -> milestoneOrchestrator.approveMilestoneSingle(p2m2.getId(), "DEMO:P2:M2"));
        settledDisbursements += settleAllDisbursementsForMilestone(p2m2, "P2M2");
        p2m2.setRetentionReleaseAt(now.plus(9, ChronoUnit.DAYS));

        MilestoneEntity p2m3 = milestones.get(6);
        runAs(contractor, () -> milestoneService.submitEvidence(p2m3.getId(), evidence("Security systems submitted")));
        runAs(owner, () -> milestoneOrchestrator.approveMilestoneSingle(p2m3.getId(), "DEMO:P2:M3"));

        // Project 3: submitted, inspected, approved pending, planned.
        MilestoneEntity p3m1 = milestones.get(8);
        runAs(contractor, () -> milestoneService.submitEvidence(p3m1.getId(), evidence("Landscaping submitted")));

        MilestoneEntity p3m2 = milestones.get(9);
        runAs(contractor, () -> milestoneService.submitEvidence(p3m2.getId(), evidence("Exterior paint submitted")));
        var inspection2 = runAs(inspector, () -> inspectionOrchestrator.schedule(new ScheduleInspectionRequest(
                p3m2.getProjectId(),
                p3m2.getId(),
                now.plus(2, ChronoUnit.DAYS),
                BigDecimal.ZERO
        )));
        runAs(inspector, () -> inspectionOrchestrator.complete(inspection2.getId(), new CompleteInspectionRequest("{\"result\":\"PASS\"}")));

        MilestoneEntity p3m3 = milestones.get(10);
        runAs(contractor, () -> milestoneService.submitEvidence(p3m3.getId(), evidence("Snag list complete")));
        runAs(owner, () -> milestoneOrchestrator.approveMilestoneSingle(p3m3.getId(), "DEMO:P3:M3"));

        return new MilestoneScenarioResult(settledDisbursements);
    }

    private SubmitMilestoneEvidenceRequest evidence(String note) {
        return new SubmitMilestoneEvidenceRequest(Map.of(
                "note", note,
                "submittedAt", Instant.now().toString()
        ), note);
    }

    private int settleAllDisbursementsForMilestone(MilestoneEntity milestone, String keyPrefix) {
        List<DisbursementOrderEntity> disbursements = disbursementRepository.findByMilestoneId(milestone.getId());
        int settled = 0;
        for (int i = 0; i < disbursements.size(); i++) {
            settleDisbursement(disbursements.get(i), keyPrefix + ":" + i);
            settled++;
        }
        return settled;
    }

    private int settleSomeDisbursementsForMilestone(MilestoneEntity milestone, String keyPrefix, int count) {
        List<DisbursementOrderEntity> disbursements = disbursementRepository.findByMilestoneId(milestone.getId());
        int settled = 0;
        for (int i = 0; i < disbursements.size() && settled < count; i++) {
            settleDisbursement(disbursements.get(i), keyPrefix + ":" + i);
            settled++;
        }
        return settled;
    }

    private void settleDisbursement(DisbursementOrderEntity disbursement, String suffix) {
        if (disbursement.getStatus() == DisbursementStatus.SETTLED) {
            return;
        }
        Instant eventTs = Instant.now();
        SettlementWebhookRequest request = new SettlementWebhookRequest(
                "DEMO-EVENT-" + suffix + "-" + disbursement.getId(),
                "DISBURSEMENT_SETTLED",
                eventTs,
                new SettlementWebhookRequest.Payload(
                        disbursement.getId(),
                        "SETTLEMENT-" + suffix
                )
        );
        try {
            String rawBody = objectMapper.writeValueAsString(request);
            String signature = signWebhook(eventTs, rawBody);
            webhookService.processSettlementEvent(request, signature, rawBody);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to settle demo disbursement", ex);
        }
    }

    private void normalizeEscrowStatuses(List<EscrowEntity> escrows, DemoUser actor, Instant now) {
        if (escrows.isEmpty()) {
            return;
        }

        int fundedApplied = 0;
        int completedApplied = 0;

        for (EscrowEntity escrow : escrows) {
            switch (escrow.getBusinessKey()) {
                case "DEMO:ESCROW:FUNDED" -> {
                    postEscrowFunded(escrow, actor, "DEMO:ESCROW:FUNDED");
                    escrow.setStatus(EscrowStatus.FUNDED);
                    fundedApplied++;
                }
                case "DEMO:ESCROW:ACTIVE" -> {
                    postEscrowFunded(escrow, actor, "DEMO:ESCROW:ACTIVE");
                    escrow.setStatus(EscrowStatus.ACTIVE);
                    fundedApplied++;
                }
                case "DEMO:ESCROW:COMPLETED" -> {
                    postEscrowFunded(escrow, actor, "DEMO:ESCROW:COMPLETED");
                    ledgerPostingService.post(ledgerTemplateService.escrowReleasedToSeller(
                            escrow.getId().toString(),
                            actor.id(),
                            escrow.getTotalAmount(),
                            escrow.getCurrency(),
                            "DEMO:ESCROW:COMPLETED:RELEASE"
                    ));
                    escrow.setStatus(EscrowStatus.COMPLETED);
                    fundedApplied++;
                    completedApplied++;
                }
                case "DEMO:ESCROW:CANCELLED" -> escrow.setStatus(EscrowStatus.CANCELLED);
                case "DEMO:ESCROW:DISPUTED" -> escrow.setStatus(EscrowStatus.DISPUTED);
                default -> {
                    if (fundedApplied < 2 && escrow.getStatus() == EscrowStatus.INITIATED) {
                        postEscrowFunded(escrow, actor, "DEMO:AUTO:FUNDED:" + fundedApplied);
                        escrow.setStatus(fundedApplied == 0 ? EscrowStatus.FUNDED : EscrowStatus.ACTIVE);
                        fundedApplied++;
                    } else if (completedApplied < 1 && escrow.getStatus() == EscrowStatus.INITIATED) {
                        postEscrowFunded(escrow, actor, "DEMO:AUTO:COMPLETE");
                        ledgerPostingService.post(ledgerTemplateService.escrowReleasedToSeller(
                                escrow.getId().toString(),
                                actor.id(),
                                escrow.getTotalAmount(),
                                escrow.getCurrency(),
                                "DEMO:AUTO:COMPLETE:RELEASE"
                        ));
                        escrow.setStatus(EscrowStatus.COMPLETED);
                        completedApplied++;
                    }
                }
            }
        }
    }

    private void postEscrowFunded(EscrowEntity escrow, DemoUser actor, String key) {
        ledgerPostingService.post(ledgerTemplateService.escrowFunded(
                escrow.getId().toString(),
                actor.id(),
                escrow.getTotalAmount(),
                escrow.getCurrency(),
                key + ":FUNDED"
        ));
    }

    private DemoUser userForProperty(PropertyEntity property, Map<String, DemoUser> users) {
        if (property.getOwnerUserId().equals(users.get("owner").id())) {
            return users.get("owner");
        }
        return users.get("seller");
    }

    private void reseedLedgerAccounts() {
        jdbcTemplate.batchUpdate("""
                INSERT INTO ledger.accounts (id, account_code, name, account_type, currency, active)
                VALUES (gen_random_uuid(), ?, ?, ?, 'TZS', TRUE)
                """, List.of(
                new Object[]{"1010", "Bank Cash", "ASSET"},
                new Object[]{"2010", "Escrow Liability", "LIABILITY"},
                new Object[]{"2030", "Contractor Payable", "LIABILITY"},
                new Object[]{"2040", "Inspector Payable", "LIABILITY"},
                new Object[]{"2050", "Seller Payable", "LIABILITY"},
                new Object[]{"2060", "Platform Fees Holding", "LIABILITY"},
                new Object[]{"2080", "Retention Payable", "LIABILITY"},
                new Object[]{"2090", "Supplier Payable", "LIABILITY"},
                new Object[]{"5020", "Penalty Income", "INCOME"}
        ));
    }

    private String signWebhook(Instant eventTs, String rawBody) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] signature = mac.doFinal((eventTs.toString() + "." + rawBody).getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(signature);
    }

    private UUID stableUuid(String seed) {
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    private <T> T runAs(DemoUser user, DemoWork<T> work) {
        var previous = SecurityContextHolder.getContext().getAuthentication();
        ActorPrincipal principal = new ActorPrincipal(user.id(), user.roles());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of())
        );
        try {
            return work.run();
        } finally {
            if (previous == null) {
                SecurityContextHolder.clearContext();
            } else {
                SecurityContextHolder.getContext().setAuthentication(previous);
            }
        }
    }

    @FunctionalInterface
    private interface DemoWork<T> {
        T run();
    }

    private record DemoUser(UUID id, String email, Set<AppRole> roles) {
    }

    private record OfferFlowResult(List<OfferEntity> offers,
                                   PropertyReservationEntity reservation,
                                   EscrowEntity purchaseEscrow) {
    }

    private record MilestoneScenarioResult(int settledDisbursements) {
    }
}
