package com.uzenjitrust.ops.bank;

public interface BankAdapter {

    BankPayoutResponse initiatePayout(BankPayoutRequest request);
}
