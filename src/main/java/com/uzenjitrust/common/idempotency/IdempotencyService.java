package com.uzenjitrust.common.idempotency;

import com.uzenjitrust.common.error.ConflictException;
import com.uzenjitrust.ledger.domain.LedgerIdempotencyKeyEntity;
import com.uzenjitrust.ledger.repo.LedgerIdempotencyRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IdempotencyService {

    private final LedgerIdempotencyRepository repository;

    public IdempotencyService(LedgerIdempotencyRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public KeyReservation reserve(String scope, String key, String fingerprint) {
        LedgerIdempotencyKeyEntity existing = repository.findByKeyScopeAndKeyValue(scope, key).orElse(null);
        if (existing != null) {
            if (!existing.getRequestFingerprint().equals(fingerprint)) {
                throw new ConflictException("Idempotency key reused with different request");
            }
            return new KeyReservation(false);
        }

        LedgerIdempotencyKeyEntity entity = new LedgerIdempotencyKeyEntity();
        entity.setKeyScope(scope);
        entity.setKeyValue(key);
        entity.setRequestFingerprint(fingerprint);
        try {
            repository.save(entity);
            return new KeyReservation(true);
        } catch (DataIntegrityViolationException ex) {
            LedgerIdempotencyKeyEntity raced = repository.findByKeyScopeAndKeyValue(scope, key).orElseThrow(() -> ex);
            if (!raced.getRequestFingerprint().equals(fingerprint)) {
                throw new ConflictException("Idempotency key reused with different request");
            }
            return new KeyReservation(false);
        }
    }

    public record KeyReservation(boolean created) {
    }
}
