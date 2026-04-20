package com.carddemo.billpay.exception;

/**
 * Base class for bill-payment errors. Each subclass corresponds to a specific
 * COBOL error site (see the traceability table in
 * {@code docs/COBIL00C_SPEC.md}) and carries the exact user-facing message
 * that COBIL00C would render on the 3270 map.
 */
public abstract class BillPaymentException extends RuntimeException {
    protected BillPaymentException(String message) {
        super(message);
    }
}
