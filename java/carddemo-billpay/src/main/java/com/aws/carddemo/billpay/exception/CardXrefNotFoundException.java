package com.aws.carddemo.billpay.exception;

public class CardXrefNotFoundException extends BillPayException {
    public CardXrefNotFoundException(String acctId) {
        super("Card cross-reference NOT found for account: " + acctId);
    }
}
