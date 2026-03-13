package com.uzenjitrust.ledger.service;

import com.uzenjitrust.common.error.ForbiddenException;
import com.uzenjitrust.common.error.NotFoundException;
import com.uzenjitrust.common.security.ActorPrincipal;
import com.uzenjitrust.common.security.ActorProvider;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.ledger.domain.HashChainEntity;
import com.uzenjitrust.ledger.domain.JournalEntryEntity;
import com.uzenjitrust.ledger.domain.JournalLineEntity;
import com.uzenjitrust.ledger.repo.HashChainRepository;
import com.uzenjitrust.ledger.repo.JournalEntryRepository;
import com.uzenjitrust.ledger.repo.JournalLineRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class LedgerReadService {

    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final HashChainRepository hashChainRepository;
    private final ActorProvider actorProvider;

    public LedgerReadService(JournalEntryRepository journalEntryRepository,
                             JournalLineRepository journalLineRepository,
                             HashChainRepository hashChainRepository,
                             ActorProvider actorProvider) {
        this.journalEntryRepository = journalEntryRepository;
        this.journalLineRepository = journalLineRepository;
        this.hashChainRepository = hashChainRepository;
        this.actorProvider = actorProvider;
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryView> listJournalEntries(String entryType,
                                                    String referenceId,
                                                    int page,
                                                    int size,
                                                    String sortBy,
                                                    Sort.Direction direction) {
        requireAdmin();
        return journalEntryRepository.findAll(
                        Specification.where(matchesString("entryType", entryType))
                                .and(matchesString("referenceId", referenceId)),
                        PageRequest.of(page, size, Sort.by(direction, sortBy)))
                .map(this::toEntryView);
    }

    @Transactional(readOnly = true)
    public LedgerEntryDetailView getJournalEntry(UUID journalEntryId) {
        requireAdmin();
        JournalEntryEntity entry = journalEntryRepository.findById(journalEntryId)
                .orElseThrow(() -> new NotFoundException("Ledger entry not found"));
        HashChainEntity chain = hashChainRepository.findByJournalEntry_Id(journalEntryId).orElse(null);
        List<JournalLineView> lines = journalLineRepository.findByJournalEntry_IdOrderByCreatedAtAsc(journalEntryId).stream()
                .map(this::toLineView)
                .toList();
        LedgerEntryView summary = toEntryView(entry);
        return new LedgerEntryDetailView(
                summary.id(),
                summary.entryType(),
                null,
                summary.referenceId(),
                summary.idempotencyKey(),
                entry.getDescription(),
                entry.getActorUserId(),
                summary.hash(),
                summary.prevHash(),
                chain == null ? null : chain.getChainIndex(),
                summary.createdAt(),
                lines
        );
    }

    private void requireAdmin() {
        ActorPrincipal actor = actorProvider.requireActor();
        if (!actor.roles().contains(AppRole.ADMIN)) {
            throw new ForbiddenException("Insufficient role");
        }
    }

    private LedgerEntryView toEntryView(JournalEntryEntity entity) {
        HashChainEntity hashChain = hashChainRepository.findByJournalEntry_Id(entity.getId()).orElse(null);
        return new LedgerEntryView(
                entity.getId(),
                entity.getEntryType(),
                null,
                entity.getReferenceId(),
                entity.getIdempotencyKey(),
                hashChain == null ? null : hashChain.getHash(),
                hashChain == null ? null : hashChain.getPrevHash(),
                entity.getCreatedAt()
        );
    }

    private JournalLineView toLineView(JournalLineEntity line) {
        return new JournalLineView(
                line.getId(),
                line.getAccount().getAccountCode(),
                line.getAccount().getName(),
                line.getLineType().name(),
                line.getAmount(),
                line.getCurrency()
        );
    }

    private static Specification<JournalEntryEntity> matchesString(String field, String value) {
        return (root, query, cb) -> value == null || value.isBlank() ? null : cb.equal(root.get(field), value);
    }

    public record LedgerEntryView(
            UUID id,
            String entryType,
            String referenceType,
            String referenceId,
            String idempotencyKey,
            String hash,
            String prevHash,
            Instant createdAt
    ) { }

    public record LedgerEntryDetailView(
            UUID id,
            String entryType,
            String referenceType,
            String referenceId,
            String idempotencyKey,
            String description,
            UUID actorUserId,
            String hash,
            String prevHash,
            Long chainIndex,
            Instant createdAt,
            List<JournalLineView> lines
    ) { }

    public record JournalLineView(
            UUID id,
            String accountCode,
            String accountName,
            String direction,
            java.math.BigDecimal amount,
            String currency
    ) { }
}
