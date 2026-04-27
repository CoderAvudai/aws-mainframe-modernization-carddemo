package com.aws.carddemo.dto;

import jakarta.validation.constraints.NotNull;

public class InterestCalculationRequest {

    @NotNull(message = "Processing date is required (format: YYYY-MM-DD)")
    private String processingDate;

    private Long accountId;

    public InterestCalculationRequest() {
    }

    public InterestCalculationRequest(String processingDate, Long accountId) {
        this.processingDate = processingDate;
        this.accountId = accountId;
    }

    public String getProcessingDate() {
        return processingDate;
    }

    public void setProcessingDate(String processingDate) {
        this.processingDate = processingDate;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }
}
