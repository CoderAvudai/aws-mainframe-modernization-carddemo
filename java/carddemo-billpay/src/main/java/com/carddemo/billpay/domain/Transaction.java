package com.carddemo.billpay.domain;

import java.math.BigDecimal;

/**
 * Mirror of the VSAM {@code TRAN-RECORD} (copybook {@code CVTRA05Y.cpy}).
 * Key is the 16-character {@code TRAN-ID}.
 */
public record Transaction(
        String transactionId,
        String typeCode,
        int categoryCode,
        String source,
        String description,
        BigDecimal amount,
        long merchantId,
        String merchantName,
        String merchantCity,
        String merchantZip,
        String cardNumber,
        String originationTimestamp,
        String processingTimestamp) {

    /** COBIL00C writes the type code {@code "02"} (bill payment). */
    public static final String TYPE_CODE_BILL_PAYMENT = "02";

    /** COBIL00C writes category code {@code 2}. */
    public static final int CATEGORY_BILL_PAYMENT = 2;

    /** Synthetic merchant id for the self-payment (R23 / I4). */
    public static final long MERCHANT_ID_BILL_PAYMENT = 999_999_999L;
}
