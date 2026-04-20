package com.carddemo.billpay.service;

/**
 * State-machine outcomes of a single bill-payment turn. Each value corresponds
 * to one of the distinguishable end-states COBIL00C can reach in a single
 * pseudo-conversational turn.
 */
public enum BillPaymentStatus {
    /** Inquiry — balance shown, awaiting Y/N confirmation (R17). */
    AWAITING_CONFIRMATION,
    /** Full-balance payment succeeded; transaction id populated (R27). */
    PAYMENT_SUCCESSFUL,
    /** Account has zero or negative balance (R16). */
    NOTHING_TO_PAY,
    /** User explicitly answered N / n (R18). */
    CANCELLED
}
