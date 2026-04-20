package com.carddemo.billpay.domain;

/**
 * Mirror of the {@code CARD-XREF-RECORD} (copybook {@code CVACT03Y.cpy}).
 * Accessed through the alternate index {@code CXACAIX} keyed by
 * {@code XREF-ACCT-ID}.
 */
public record CardXref(String cardNumber, String customerId, String accountId) {
}
