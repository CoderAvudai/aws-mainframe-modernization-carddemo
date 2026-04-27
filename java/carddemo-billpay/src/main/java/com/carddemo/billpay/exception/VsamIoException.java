package com.carddemo.billpay.exception;

/**
 * R29 / R31 / R33 / R35 / R37 / R39 — generic I/O failure on a VSAM dataset.
 * Each subclass carries the exact user message used by COBIL00C so that the
 * HTTP layer can reproduce the same operator-visible text.
 */
public class VsamIoException extends BillPaymentException {

    public VsamIoException(String message) {
        super(message);
    }

    public static VsamIoException lookupAccount() {
        return new VsamIoException("Unable to lookup Account...");
    }

    public static VsamIoException updateAccount() {
        return new VsamIoException("Unable to Update Account...");
    }

    public static VsamIoException lookupXref() {
        return new VsamIoException("Unable to lookup XREF AIX file...");
    }

    public static VsamIoException lookupTransaction() {
        return new VsamIoException("Unable to lookup Transaction...");
    }

    public static VsamIoException writeTransaction() {
        return new VsamIoException("Unable to Add Bill pay Transaction...");
    }
}
