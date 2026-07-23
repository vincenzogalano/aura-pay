package com.aurapay.banksimulator.controller;

import com.aurapay.banksimulator.dto.request.BankAuthorizationRequest;
import com.aurapay.banksimulator.dto.response.BankAuthorizationResponse;
import com.aurapay.banksimulator.dto.request.BankRefundRequest;
import com.aurapay.banksimulator.dto.response.BankRefundResponse;
import com.aurapay.banksimulator.service.BankSimulatorService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/bank")
public class BankSimulatorController {

    private final BankSimulatorService bankSimulatorService;

    public BankSimulatorController(BankSimulatorService bankSimulatorService) {
        this.bankSimulatorService = bankSimulatorService;
    }

    @PostMapping("/authorize")
    public ResponseEntity<BankAuthorizationResponse> authorize(@Valid @RequestBody BankAuthorizationRequest request) {
        BankAuthorizationResponse response = bankSimulatorService.authorize(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refund")
    public ResponseEntity<BankRefundResponse> refund(@Valid @RequestBody BankRefundRequest request) {
        BankRefundResponse response = bankSimulatorService.refund(request);
        return ResponseEntity.ok(response);
    }
}
