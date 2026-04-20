package com.carddemo.billpay.service;

import java.math.BigDecimal;

/**
 * Output of a single bill-payment turn — the Java analogue of the
 * {@code COBIL0AO} output map combined with {@code WS-MESSAGE}.
 *
 * <p>Only fields relevant to each {@link BillPaymentStatus} are populated; the
 * rest will be {@code null}. Callers should switch on {@link #status()}
 * before interpreting the other fields.
 */
public record BillPaymentResponse(
        BillPaymentStatus status,
        String accountId,
        BigDecimal currentBalance,
        String transactionId,
        String message) {
}
