package com.aws.carddemo.billpay.model;

import jakarta.validation.constraints.NotBlank;

public record BillPayRequest(
        @NotBlank(message = "Acct ID can NOT be empty")
        String accountId,
        String confirm
) {}
