package com.carddemo.billpay.web;

/** Error body returned by {@link GlobalExceptionHandler}. */
public record BillPaymentErrorResponse(String error, String message) {
}
