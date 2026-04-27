package com.aws.carddemo.billpay.exception;

public class InvalidConfirmationException extends BillPayException {
    public InvalidConfirmationException() {
        super("Invalid value. Valid values are (Y/N)");
    }
}
