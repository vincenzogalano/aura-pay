package com.aurapay.banksimulator.service;

import com.aurapay.banksimulator.dto.request.BankAuthorizationRequest;
import com.aurapay.banksimulator.dto.response.BankAuthorizationResponse;
import com.aurapay.banksimulator.dto.request.BankRefundRequest;
import com.aurapay.banksimulator.dto.response.BankRefundResponse;
import com.aurapay.banksimulator.publisher.BankEventPublisher;
import com.aurapay.banksimulator.util.LatencyUtils;
import com.aurapay.core.events.BankResponseCode;
import com.aurapay.core.exception.AuraErrorCode;
import com.aurapay.core.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class BankSimulatorService {

    private static final Logger log = LoggerFactory.getLogger(BankSimulatorService.class);

    private final BankEventPublisher eventPublisher;
    private final long simulatedLatencyMs;

    public BankSimulatorService(
            BankEventPublisher eventPublisher,
            @Value("${aurapay.bank.simulated-latency-ms:100}") long simulatedLatencyMs) {
        this.eventPublisher = eventPublisher;
        this.simulatedLatencyMs = simulatedLatencyMs;
    }

    public BankAuthorizationResponse authorize(BankAuthorizationRequest request) {
        log.info("Processing authorization request for PaymentIntent: {}, amount: {}", 
                request.paymentIntentId(), request.amountCents());

        LatencyUtils.simulateLatency(simulatedLatencyMs);

        long amount = request.amountCents();
        long lastTwoDigits = Math.abs(amount) % 100;

        BankAuthorizationResponse response;

        if (lastTwoDigits == 95) {
            log.warn("Simulating bank timeout/unavailability for amount: {}", amount);
            LatencyUtils.simulateLatency(3500);
            throw new BusinessException(AuraErrorCode.BANK_UNAVAILABLE, "Acquiring bank timeout simulated");
        } else if (lastTwoDigits == 99) {
            response = BankAuthorizationResponse.declined(
                    BankResponseCode.INSUFFICIENT_FUNDS.getCode(), 
                    BankResponseCode.INSUFFICIENT_FUNDS.getDescription()
            );
        } else if (lastTwoDigits == 98) {
            response = BankAuthorizationResponse.declined(
                    BankResponseCode.EXPIRED_CARD.getCode(), 
                    BankResponseCode.EXPIRED_CARD.getDescription()
            );
        } else if (lastTwoDigits == 97) {
            response = BankAuthorizationResponse.declined(
                    BankResponseCode.SUSPECTED_FRAUD.getCode(), 
                    BankResponseCode.SUSPECTED_FRAUD.getDescription()
            );
        } else {
            String txId = "tx_bank_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            String authCode = "AUTH_" + (100000 + (int)(Math.random() * 900000));
            response = BankAuthorizationResponse.approved(txId, authCode);
        }

        eventPublisher.publishAuthorizationResult(request, response);

        return response;
    }

    public BankRefundResponse refund(BankRefundRequest request) {
        log.info("Processing refund request for Bank Tx: {}, amount: {}", 
                request.originalTransactionId(), request.amountCents());

        LatencyUtils.simulateLatency(simulatedLatencyMs);

        long amount = request.amountCents();
        if (Math.abs(amount) % 100 == 99) {
            return BankRefundResponse.failed(BankResponseCode.INSUFFICIENT_FUNDS.getCode());
        }

        String refundTxId = "tx_ref_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        return BankRefundResponse.ok(refundTxId);
    }
}
