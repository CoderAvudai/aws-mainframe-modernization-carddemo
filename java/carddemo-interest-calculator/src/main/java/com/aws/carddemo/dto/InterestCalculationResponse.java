package com.aws.carddemo.dto;

import java.util.List;

public class InterestCalculationResponse {

    private String status;
    private String processingDate;
    private int recordsProcessed;
    private List<InterestResult> results;

    public InterestCalculationResponse() {
    }

    public InterestCalculationResponse(String status, String processingDate,
                                        int recordsProcessed,
                                        List<InterestResult> results) {
        this.status = status;
        this.processingDate = processingDate;
        this.recordsProcessed = recordsProcessed;
        this.results = results;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProcessingDate() {
        return processingDate;
    }

    public void setProcessingDate(String processingDate) {
        this.processingDate = processingDate;
    }

    public int getRecordsProcessed() {
        return recordsProcessed;
    }

    public void setRecordsProcessed(int recordsProcessed) {
        this.recordsProcessed = recordsProcessed;
    }

    public List<InterestResult> getResults() {
        return results;
    }

    public void setResults(List<InterestResult> results) {
        this.results = results;
    }
}
