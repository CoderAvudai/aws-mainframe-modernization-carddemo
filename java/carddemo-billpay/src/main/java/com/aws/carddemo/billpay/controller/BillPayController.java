package com.aws.carddemo.billpay.controller;

import com.aws.carddemo.billpay.model.BalanceInquiryResponse;
import com.aws.carddemo.billpay.model.BillPayRequest;
import com.aws.carddemo.billpay.model.BillPayResponse;
import com.aws.carddemo.billpay.service.BillPayService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billpay")
public class BillPayController {

    private final BillPayService billPayService;

    public BillPayController(BillPayService billPayService) {
        this.billPayService = billPayService;
    }

    /**
     * Rule 4.4 / 5.1: Look up the account balance before confirming payment.
     */
    @GetMapping("/accounts/{accountId}/balance")
    public ResponseEntity<BalanceInquiryResponse> getBalance(
            @PathVariable String accountId) {
        return ResponseEntity.ok(billPayService.lookupBalance(accountId));
    }

    /**
     * Rules 3-10: Process a bill payment (initial inquiry or confirmed payment).
     */
    @PostMapping
    public ResponseEntity<BillPayResponse> pay(
            @Valid @RequestBody BillPayRequest request) {
        BillPayResponse response = billPayService.processPayment(
                request.accountId(), request.confirm());
        return ResponseEntity.ok(response);
    }
}
