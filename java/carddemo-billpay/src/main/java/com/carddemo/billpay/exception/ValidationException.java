package com.carddemo.billpay.exception;

/**
 * R11 / R12 — user-input validation error. Distinct from VSAM errors so the
 * controller can return HTTP 400 instead of 500.
 */
public class ValidationException extends BillPaymentException {
    public static final String EMPTY_ACCT_ID = "Acct ID can NOT be empty...";
    public static final String INVALID_CONFIRM =
            "Invalid value. Valid values are (Y/N)...";

    public ValidationException(String message) {
        super(message);
    }
}
