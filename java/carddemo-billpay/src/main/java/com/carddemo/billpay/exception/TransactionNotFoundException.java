package com.carddemo.billpay.exception;

/** R34 — STARTBR on TRANSACT returned NOTFND. */
public class TransactionNotFoundException extends BillPaymentException {
    public static final String MESSAGE = "Transaction ID NOT found...";

    public TransactionNotFoundException() {
        super(MESSAGE);
    }
}
