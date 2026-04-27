package com.aws.carddemo.billpay.exception;

public class DuplicateTransactionException extends BillPayException {
    public DuplicateTransactionException(String tranId) {
        super("Tran ID already exist: " + tranId);
    }
}
