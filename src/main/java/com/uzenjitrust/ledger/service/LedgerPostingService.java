package com.uzenjitrust.ledger.service;

import com.uzenjitrust.common.error.BadRequestException;
import com.uzenjitrust.common.error.NotFoundException;
import com.uzenjitrust.ledger.domain.HashChainEntity;
import com.uzenjitrust.ledger.domain.JournalEntryEntity;
import com.uzenjitrust.ledger.domain.JournalLineEntity;
import com.uzenjitrust.ledger.domain.LedgerAccountEntity;
import com.uzenjitrust.ledger.domain.LineType;
import com.uzenjitrust.ledger.repo.HashChainRepository;
import com.uzenjitrust.ledger.repo.JournalEntryRepository;
import com.uzenjitrust.ledger.repo.JournalLineRepository;
import com.uzenjitrust.ledger.repo.LedgerAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LedgerPostingService {

    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;
    private final LedgerAccountRepository accountRepository;
    private final HashChainRepository hashChainRepository;

    public LedgerPostingService(JournalEntryRepository journalEntryRepository,
                                JournalLineRepository journalLineRepository,
                                LedgerAccountRepository accountRepository,
                                HashChainRepository hashChainRepository) {
        this.journalEntryRepository = journalEntryRepository;
        this.journalLineRepository = journalLineRepository;
        this.accountRepository = accountRepository;
        this.hashChainRepository = hashChainRepository;
    }

    @Transactional
    public LedgerPostingResult post(LedgerPostingRequest request) {
        validateRequest(request);

        JournalEntryEntity existing = journalEntryRepository
                .findByEntryTypeAndReferenceIdAndIdempotencyKey(
                        request.entryType(),
                        request.referenceId(),
                        request.idempotencyKey()
                )
                .orElse(null);
        if (existing != null) {
            return toResult(existing, true);
        }

        Map<String, LedgerAccountEntity> accountsByCode = resolveAccounts(request.lines());

        Instant now = Instant.now();
        JournalEntryEntity entry = new JournalEntryEntity();
        entry.setId(UUID.randomUUID());
        entry.setEntryType(request.entryType());
        entry.setReferenceId(request.referenceId());
        entry.setIdempotencyKey(request.idempotencyKey());
        entry.setDescription(request.description());
        entry.setActorUserId(request.actorUserId());
        entry.setCreatedAt(now);

        try {
            journalEntryRepository.saveAndFlush(entry);

            List<JournalLineEntity> journalLines = request.lines().stream().map(line -> {
                JournalLineEntity entity = new JournalLineEntity();
                entity.setId(UUID.randomUUID());
                entity.setJournalEntry(entry);
                entity.setAccount(accountsByCode.get(line.accountCode()));
                entity.setLineType(line.lineType());
                entity.setAmount(line.amount());
                entity.setCurrency(line.currency());
                entity.setCreatedAt(now);
                return entity;
            }).toList();
            journalLineRepository.saveAll(journalLines);

            hashChainRepository.lockChainTable();
            HashChainEntity tail = hashChainRepository.findTail().orElse(null);
            long nextIndex = tail == null ? 1L : tail.getChainIndex() + 1;
            String prevHash = tail == null ? null : tail.getHash();
            String hash = computeHash(request, nextIndex, prevHash);

            HashChainEntity chain = new HashChainEntity();
            chain.setJournalEntry(entry);
            chain.setChainIndex(nextIndex);
            chain.setPrevHash(prevHash);
            chain.setHash(hash);
            chain.setCreatedAt(now);
            hashChainRepository.save(chain);

            return new LedgerPostingResult(entry.getId(), nextIndex, hash, prevHash, now, false);
        } catch (DataIntegrityViolationException ex) {
            JournalEntryEntity raced = journalEntryRepository
                    .findByEntryTypeAndReferenceIdAndIdempotencyKey(
                            request.entryType(),
                            request.referenceId(),
                            request.idempotencyKey())
                    .orElseThrow(() -> ex);
            return toResult(raced, true);
        }
    }

    private Map<String, LedgerAccountEntity> resolveAccounts(List<LedgerPostingLine> lines) {
        return lines.stream()
                .map(LedgerPostingLine::accountCode)
                .distinct()
                .map(code -> accountRepository.findByAccountCodeAndActiveTrue(code)
                        .orElseThrow(() -> new NotFoundException("Ledger account not found: " + code)))
                .collect(Collectors.toMap(LedgerAccountEntity::getAccountCode, Function.identity()));
    }

    private void validateRequest(LedgerPostingRequest request) {
        if (request.entryType() == null || request.entryType().isBlank()) {
            throw new BadRequestException("entryType is required");
        }
        if (request.referenceId() == null || request.referenceId().isBlank()) {
            throw new BadRequestException("referenceId is required");
        }
        if (request.idempotencyKey() == null || request.idempotencyKey().isBlank()) {
            throw new BadRequestException("idempotencyKey is required");
        }
        if (request.lines() == null || request.lines().size() < 2) {
            throw new BadRequestException("At least two journal lines are required");
        }

        for (LedgerPostingLine line : request.lines()) {
            if (line.accountCode() == null || line.accountCode().isBlank()) {
                throw new BadRequestException("accountCode is required");
            }
            if (line.lineType() == null) {
                throw new BadRequestException("lineType is required");
            }
            if (line.amount() == null || line.amount().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("amount must be positive");
            }
            if (line.currency() == null || line.currency().isBlank()) {
                throw new BadRequestException("currency is required");
            }
        }

        Map<String, BigDecimal> debitByCurrency = request.lines().stream()
                .filter(line -> line.lineType() == LineType.DEBIT)
                .collect(Collectors.groupingBy(LedgerPostingLine::currency,
                        Collectors.mapping(LedgerPostingLine::amount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        Map<String, BigDecimal> creditByCurrency = request.lines().stream()
                .filter(line -> line.lineType() == LineType.CREDIT)
                .collect(Collectors.groupingBy(LedgerPostingLine::currency,
                        Collectors.mapping(LedgerPostingLine::amount,
                                Collectors.reducing(BigDecimal.ZERO, BigDecimal::add))));

        if (!debitByCurrency.equals(creditByCurrency)) {
            throw new BadRequestException("Ledger entry is not balanced");
        }
    }

    private String computeHash(LedgerPostingRequest request, long chainIndex, String prevHash) {
        String canonicalLines = request.lines().stream()
                .sorted(Comparator
                        .comparing(LedgerPostingLine::accountCode)
                        .thenComparing(l -> l.lineType().name())
                        .thenComparing(LedgerPostingLine::currency)
                        .thenComparing(LedgerPostingLine::amount))
                .map(line -> String.join(":",
                        line.accountCode(),
                        line.lineType().name(),
                        line.amount().toPlainString(),
                        line.currency()))
                .collect(Collectors.joining("|"));

        String payload = String.join("#",
                String.valueOf(chainIndex),
                request.entryType(),
                request.referenceId(),
                request.idempotencyKey(),
                String.valueOf(request.actorUserId()),
                prevHash == null ? "GENESIS" : prevHash,
                canonicalLines
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to compute ledger hash", ex);
        }
    }

    private LedgerPostingResult toResult(JournalEntryEntity entry, boolean reused) {
        HashChainEntity hashChain = hashChainRepository.findByJournalEntry_Id(entry.getId())
                .orElseThrow(() -> new NotFoundException("Hash chain record missing for entry " + entry.getId()));
        return new LedgerPostingResult(
                entry.getId(),
                hashChain.getChainIndex(),
                hashChain.getHash(),
                hashChain.getPrevHash(),
                entry.getCreatedAt(),
                reused
        );
    }
}
