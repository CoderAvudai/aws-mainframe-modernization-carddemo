package com.aws.carddemo.dto;

import java.math.BigDecimal;

public class InterestResult {

    private Long accountId;
    private BigDecimal totalInterest;
    private BigDecimal updatedBalance;
    private int transactionsGenerated;

    public InterestResult() {
    }

    public InterestResult(Long accountId, BigDecimal totalInterest,
                          BigDecimal updatedBalance, int transactionsGenerated) {
        this.accountId = accountId;
        this.totalInterest = totalInterest;
        this.updatedBalance = updatedBalance;
        this.transactionsGenerated = transactionsGenerated;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getTotalInterest() {
        return totalInterest;
    }

    public void setTotalInterest(BigDecimal totalInterest) {
        this.totalInterest = totalInterest;
    }

    public BigDecimal getUpdatedBalance() {
        return updatedBalance;
    }

    public void setUpdatedBalance(BigDecimal updatedBalance) {
        this.updatedBalance = updatedBalance;
    }

    public int getTransactionsGenerated() {
        return transactionsGenerated;
    }

    public void setTransactionsGenerated(int transactionsGenerated) {
        this.transactionsGenerated = transactionsGenerated;
    }
}
