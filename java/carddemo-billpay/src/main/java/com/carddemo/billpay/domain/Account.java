package com.carddemo.billpay.domain;

import java.math.BigDecimal;

/**
 * Mirror of the VSAM {@code ACCOUNT-RECORD} described by copybook
 * {@code CVACT01Y.cpy}. Only fields actually read or written by COBIL00C are
 * required for business logic; the rest are preserved as-is for round-trip
 * fidelity.
 *
 * <p>Field lengths follow the COBOL picture clauses: the {@code accountId} is
 * 11 digits, monetary fields are {@code S9(10)V99} (two decimal places).
 */
public record Account(
        String accountId,
        String activeStatus,
        BigDecimal currentBalance,
        BigDecimal creditLimit,
        BigDecimal cashCreditLimit,
        String openDate,
        String expirationDate,
        String reissueDate,
        BigDecimal currentCycleCredit,
        BigDecimal currentCycleDebit,
        String addressZip,
        String groupId) {

    /** Returns a copy of this account with a new current balance (REWRITE). */
    public Account withCurrentBalance(BigDecimal newBalance) {
        return new Account(
                accountId,
                activeStatus,
                newBalance,
                creditLimit,
                cashCreditLimit,
                openDate,
                expirationDate,
                reissueDate,
                currentCycleCredit,
                currentCycleDebit,
                addressZip,
                groupId);
    }
}
