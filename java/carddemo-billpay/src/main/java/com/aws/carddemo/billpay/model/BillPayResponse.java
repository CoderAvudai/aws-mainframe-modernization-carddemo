package com.aws.carddemo.billpay.model;

import java.math.BigDecimal;

public record BillPayResponse(
        String message,
        String transactionId,
        BigDecimal previousBalance,
        BigDecimal newBalance
) {}
