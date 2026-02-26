package com.uzenjitrust.ops.bank;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

@Component
@Profile({"local", "default", "test"})
public class MockBankAdapter implements BankAdapter {

    @Override
    public BankPayoutResponse initiatePayout(BankPayoutRequest request) {
        return new BankPayoutResponse("MOCK-BANK-" + shortHash(request.idempotencyKey()));
    }

    private String shortHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to generate deterministic mock bank reference", ex);
        }
    }
}
