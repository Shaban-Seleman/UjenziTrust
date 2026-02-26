package com.uzenjitrust.ops.bank;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile({"local", "default", "test"})
public class MockBankAdapter implements BankAdapter {

    @Override
    public BankPayoutResponse initiatePayout(BankPayoutRequest request) {
        return new BankPayoutResponse("MOCK-BANK-" + UUID.randomUUID());
    }
}
