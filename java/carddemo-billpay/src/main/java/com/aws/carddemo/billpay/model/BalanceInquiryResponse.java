package com.aws.carddemo.billpay.model;

import java.math.BigDecimal;

public record BalanceInquiryResponse(
        String accountId,
        BigDecimal currentBalance,
        String message
) {}
