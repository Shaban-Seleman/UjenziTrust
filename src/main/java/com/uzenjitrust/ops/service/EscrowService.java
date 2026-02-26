package com.uzenjitrust.ops.service;

import com.uzenjitrust.ops.domain.EscrowEntity;
import com.uzenjitrust.ops.domain.EscrowStatus;
import com.uzenjitrust.ops.repo.EscrowRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class EscrowService {

    private final EscrowRepository escrowRepository;

    public EscrowService(EscrowRepository escrowRepository) {
        this.escrowRepository = escrowRepository;
    }

    @Transactional
    public EscrowEntity createEscrowIdempotent(String businessKey,
                                               String escrowType,
                                               BigDecimal totalAmount,
                                               String currency,
                                               UUID payerUserId,
                                               UUID beneficiaryUserId) {
        return escrowRepository.findByBusinessKey(businessKey).orElseGet(() -> {
            EscrowEntity escrow = new EscrowEntity();
            escrow.setBusinessKey(businessKey);
            escrow.setEscrowType(escrowType);
            escrow.setTotalAmount(totalAmount);
            escrow.setCurrency(currency);
            escrow.setStatus(EscrowStatus.INITIATED);
            escrow.setPayerUserId(payerUserId);
            escrow.setBeneficiaryUserId(beneficiaryUserId);
            try {
                return escrowRepository.save(escrow);
            } catch (DataIntegrityViolationException ex) {
                return escrowRepository.findByBusinessKey(businessKey).orElseThrow(() -> ex);
            }
        });
    }
}
