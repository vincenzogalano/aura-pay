package com.aurapay.orchestrator.controller;

import com.aurapay.orchestrator.dto.request.ConfirmPaymentIntentRequest;
import com.aurapay.orchestrator.dto.request.CreatePaymentIntentRequest;
import com.aurapay.orchestrator.dto.request.RefundPaymentRequest;
import com.aurapay.orchestrator.dto.response.PaymentIntentResponse;
import com.aurapay.orchestrator.service.PaymentOrchestrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentOrchestrationService paymentOrchestrationService;

    @GetMapping
    public ResponseEntity<List<PaymentIntentResponse>> getPayments(
            @RequestParam(value = "isTest", required = false) Boolean isTest) {
        List<PaymentIntentResponse> list = paymentOrchestrationService.getPayments(isTest);
        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<PaymentIntentResponse> createPaymentIntent(@Valid @RequestBody CreatePaymentIntentRequest request) {
        PaymentIntentResponse response = paymentOrchestrationService.createPaymentIntent(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{id}/confirm")
    public ResponseEntity<PaymentIntentResponse> confirmPayment(
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmPaymentIntentRequest request) {
        PaymentIntentResponse response = paymentOrchestrationService.confirmPayment(id, request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PaymentIntentResponse> getPaymentById(@PathVariable UUID id) {
        PaymentIntentResponse response = paymentOrchestrationService.getPaymentById(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<PaymentIntentResponse> cancelPayment(@PathVariable UUID id) {
        PaymentIntentResponse response = paymentOrchestrationService.cancelPayment(id);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/refund")
    public ResponseEntity<PaymentIntentResponse> refundPayment(
            @PathVariable UUID id,
            @Valid @RequestBody RefundPaymentRequest request) {
        PaymentIntentResponse response = paymentOrchestrationService.refundPayment(id, request);
        return ResponseEntity.ok(response);
    }
}
