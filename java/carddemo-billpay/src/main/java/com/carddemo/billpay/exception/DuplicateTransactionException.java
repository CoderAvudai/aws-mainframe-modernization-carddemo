package com.carddemo.billpay.exception;

/** R38 — WRITE to TRANSACT returned DUPKEY or DUPREC. */
public class DuplicateTransactionException extends BillPaymentException {
    public static final String MESSAGE = "Tran ID already exist...";

    public DuplicateTransactionException() {
        super(MESSAGE);
    }
}
