package com.aws.carddemo.controller;

import com.aws.carddemo.dto.InterestCalculationRequest;
import com.aws.carddemo.dto.InterestCalculationResponse;
import com.aws.carddemo.service.InterestCalculationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interest")
public class InterestCalculationController {

    private final InterestCalculationService service;

    public InterestCalculationController(InterestCalculationService service) {
        this.service = service;
    }

    @PostMapping("/calculate")
    public ResponseEntity<InterestCalculationResponse> calculateAll(
            @Valid @RequestBody InterestCalculationRequest request) {
        InterestCalculationResponse response =
                service.calculateInterest(request.getProcessingDate());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/calculate/{accountId}")
    public ResponseEntity<InterestCalculationResponse> calculateForAccount(
            @PathVariable Long accountId,
            @Valid @RequestBody InterestCalculationRequest request) {
        InterestCalculationResponse response =
                service.calculateInterestForAccount(accountId, request.getProcessingDate());
        return ResponseEntity.ok(response);
    }
}
