package com.carddemo.billpay.exception;

/** R28 / R30 / R32 — account (or xref) lookup returned NOTFND. */
public class AccountNotFoundException extends BillPaymentException {
    public static final String MESSAGE = "Account ID NOT found...";

    public AccountNotFoundException() {
        super(MESSAGE);
    }
}
