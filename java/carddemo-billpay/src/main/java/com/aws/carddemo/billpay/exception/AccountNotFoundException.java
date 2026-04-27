package com.aws.carddemo.billpay.exception;

public class AccountNotFoundException extends BillPayException {
    public AccountNotFoundException(String acctId) {
        super("Account ID NOT found: " + acctId);
    }
}
