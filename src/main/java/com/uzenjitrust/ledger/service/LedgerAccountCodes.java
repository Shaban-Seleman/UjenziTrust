package com.uzenjitrust.ledger.service;

public final class LedgerAccountCodes {

    public static final String BANK_CASH = "1010";
    public static final String ESCROW_LIABILITY = "2010";
    public static final String PAYABLE_CONTRACTOR = "2030";
    public static final String PAYABLE_INSPECTOR = "2040";
    public static final String PAYABLE_SELLER = "2050";
    public static final String PAYABLE_RETENTION = "2080";
    public static final String PAYABLE_SUPPLIER = "2090";

    public static String payableForPayeeType(String payeeType) {
        return switch (payeeType) {
            case "CONTRACTOR" -> PAYABLE_CONTRACTOR;
            case "SUPPLIER" -> PAYABLE_SUPPLIER;
            case "INSPECTOR" -> PAYABLE_INSPECTOR;
            case "SELLER" -> PAYABLE_SELLER;
            case "RETENTION" -> PAYABLE_RETENTION;
            default -> throw new IllegalArgumentException("Unknown payeeType: " + payeeType);
        };
    }

    private LedgerAccountCodes() {
    }
}
