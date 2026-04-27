package com.carddemo.billpay.service;

/**
 * Input to the bill-payment state machine — the Java analogue of the
 * {@code COBIL0AI} input map:
 *
 * <ul>
 *   <li>{@code accountId} maps to {@code ACTIDINI}.</li>
 *   <li>{@code confirm} maps to {@code CONFIRMI}. Accepted values, per R7/R12,
 *       are {@code "Y"}, {@code "y"}, {@code "N"}, {@code "n"}, empty, or
 *       {@code null}. Anything else is a validation error.</li>
 * </ul>
 */
public record BillPaymentRequest(String accountId, String confirm) {
}
