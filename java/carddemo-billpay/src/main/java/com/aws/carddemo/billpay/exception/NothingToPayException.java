package com.aws.carddemo.billpay.exception;

public class NothingToPayException extends BillPayException {
    public NothingToPayException() {
        super("You have nothing to pay");
    }
}
