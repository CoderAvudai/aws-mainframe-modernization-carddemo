package com.carddemo.billpay.web;

import com.carddemo.billpay.service.BillPaymentRequest;
import com.carddemo.billpay.service.BillPaymentResponse;
import com.carddemo.billpay.service.BillPaymentService;
import com.carddemo.billpay.service.BillPaymentStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP front-door for the bill-payment state machine. The COBOL original is
 * a 3270/BMS conversation — each HTTP call here corresponds to one
 * pseudo-conversational turn (R10).
 */
@RestController
@RequestMapping(value = "/api/billpay", produces = MediaType.APPLICATION_JSON_VALUE)
public class BillPaymentController {

    private final BillPaymentService service;

    public BillPaymentController(BillPaymentService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BillPaymentResponse> process(@RequestBody BillPaymentRequest request) {
        return ResponseEntity.ok(service.process(request));
    }

    /** PF4 equivalent — clears all fields, returns a fresh blank screen. */
    @PostMapping("/clear")
    public ResponseEntity<BillPaymentResponse> clear() {
        return ResponseEntity.ok(new BillPaymentResponse(
                BillPaymentStatus.AWAITING_CONFIRMATION, null, null, null, null));
    }
}
