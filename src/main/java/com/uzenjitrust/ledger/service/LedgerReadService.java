package com.uzenjitrust.ledger.service;

import com.uzenjitrust.common.error.ForbiddenException;
import com.uzenjitrust.common.security.ActorPrincipal;
import com.uzenjitrust.common.security.ActorProvider;
import com.uzenjitrust.common.security.AppRole;
import com.uzenjitrust.ledger.domain.JournalEntryEntity;
import com.uzenjitrust.ledger.repo.JournalEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class LedgerReadService {

    private final JournalEntryRepository journalEntryRepository;
    private final ActorProvider actorProvider;

    public LedgerReadService(JournalEntryRepository journalEntryRepository,
                             ActorProvider actorProvider) {
        this.journalEntryRepository = journalEntryRepository;
        this.actorProvider = actorProvider;
    }

    @Transactional(readOnly = true)
    public Page<LedgerEntryView> listJournalEntries(int page, int size, String sortBy, Sort.Direction direction) {
        ActorPrincipal actor = actorProvider.requireActor();
        if (!actor.roles().contains(AppRole.ADMIN)) {
            throw new ForbiddenException("Insufficient role");
        }
        return journalEntryRepository.findAll(PageRequest.of(page, size, Sort.by(direction, sortBy)))
                .map(LedgerEntryView::fromEntity);
    }

    public record LedgerEntryView(
            UUID id,
            String entryType,
            String referenceId,
            String idempotencyKey,
            String hash,
            String prevHash,
            Instant createdAt
    ) {
        public static LedgerEntryView fromEntity(JournalEntryEntity entity) {
            return new LedgerEntryView(
                    entity.getId(),
                    entity.getEntryType(),
                    entity.getReferenceId(),
                    entity.getIdempotencyKey(),
                    null,
                    null,
                    entity.getCreatedAt()
            );
        }
    }
}
