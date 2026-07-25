package com.aurapay.orchestrator.service;

import com.aurapay.core.enums.PaymentFailureCode;
import com.aurapay.core.exception.DomainRuleViolationException;
import com.aurapay.core.exception.ResourceNotFoundException;
import com.aurapay.orchestrator.client.BankSimulatorClient;
import com.aurapay.orchestrator.client.VaultServiceClient;
import com.aurapay.orchestrator.client.dto.BankAuthorizationRequest;
import com.aurapay.orchestrator.client.dto.BankAuthorizationResponse;
import com.aurapay.orchestrator.client.dto.VaultCardDetailsResponse;
import com.aurapay.orchestrator.domain.OutboxEvent;
import com.aurapay.orchestrator.domain.PaymentIntent;
import com.aurapay.orchestrator.domain.enums.PaymentStatus;
import com.aurapay.orchestrator.dto.request.ConfirmPaymentIntentRequest;
import com.aurapay.orchestrator.dto.request.CreatePaymentIntentRequest;
import com.aurapay.orchestrator.dto.response.PaymentIntentResponse;
import com.aurapay.orchestrator.repository.OutboxEventRepository;
import com.aurapay.orchestrator.repository.PaymentIntentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOrchestrationService {

    private final PaymentIntentRepository paymentIntentRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final PaymentEventFactory paymentEventFactory;
    private final VaultServiceClient vaultServiceClient;
    private final BankSimulatorClient bankSimulatorClient;

    @Transactional
    public PaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request) {
        log.info("Creating PaymentIntent for merchantId: {}, amount: {} cents", request.merchantId(), request.amountCents());

        PaymentIntent intent = PaymentIntent.builder()
                .merchantId(request.merchantId())
                .amountCents(request.amountCents())
                .currency(request.currency() != null ? request.currency() : "EUR")
                .description(request.description())
                .isTest(request.isTest() != null ? request.isTest() : true)
                .status(PaymentStatus.CREATED)
                .build();

        PaymentIntent savedIntent = paymentIntentRepository.save(intent);

        OutboxEvent createdOutboxEvent = paymentEventFactory.buildCreatedOutboxEvent(savedIntent);
        outboxEventRepository.save(createdOutboxEvent);

        log.info("PaymentIntent created successfully with id: {} and outbox event saved", savedIntent.getId());
        return PaymentIntentResponse.fromEntity(savedIntent);
    }

    public PaymentIntentResponse confirmPayment(UUID id, ConfirmPaymentIntentRequest request) {
        log.info("Confirming PaymentIntent id: {}", id);

        PaymentIntent intent = startProcessingTransaction(id, request.paymentMethodToken());

        VaultCardDetailsResponse cardDetails = vaultServiceClient.retrieveCardDetails(request.paymentMethodToken());
        log.info("Card details retrieved for token {}, masked PAN: {}", request.paymentMethodToken(), cardDetails.maskedPan());

        BankAuthorizationRequest bankRequest = new BankAuthorizationRequest(
                intent.getId(),
                intent.getMerchantId(),
                intent.getAmountCents(),
                intent.getCurrency(),
                request.paymentMethodToken(),
                intent.isTest()
        );

        BankAuthorizationResponse bankResponse = bankSimulatorClient.authorizePayment(bankRequest);

        return completePaymentTransaction(id, cardDetails, bankResponse);
    }

    @Transactional
    public PaymentIntent startProcessingTransaction(UUID id, String paymentMethodToken) {
        PaymentIntent intent = paymentIntentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentIntent with id '" + id + "' not found"));

        if (intent.getStatus() != PaymentStatus.CREATED) {
            log.warn("Cannot confirm PaymentIntent {} in state: {}", id, intent.getStatus());
            throw new DomainRuleViolationException("PaymentIntent is not in CREATED state, current state: " + intent.getStatus());
        }

        intent.setStatus(PaymentStatus.PROCESSING);
        intent.setPaymentMethodToken(paymentMethodToken);
        PaymentIntent saved = paymentIntentRepository.save(intent);

        OutboxEvent processingOutboxEvent = paymentEventFactory.buildProcessingOutboxEvent(saved);
        outboxEventRepository.save(processingOutboxEvent);

        return saved;
    }

    @Transactional
    public PaymentIntentResponse completePaymentTransaction(UUID id, VaultCardDetailsResponse cardDetails, BankAuthorizationResponse bankResponse) {
        PaymentIntent intent = paymentIntentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentIntent with id '" + id + "' not found"));

        if (bankResponse != null && bankResponse.authorized()) {
            log.info("PaymentIntent {} authorized by bank. TransactionId: {}", id, bankResponse.transactionId());
            intent.setStatus(PaymentStatus.SUCCEEDED);
            intent.setAuthorizationCode(bankResponse.authorizationCode());
            intent.setTransactionId(bankResponse.transactionId());
            intent.setFailureReason(null);

            PaymentIntent finalIntent = paymentIntentRepository.save(intent);

            String lastFour = extractLastFour(cardDetails != null ? cardDetails.maskedPan() : null);
            OutboxEvent succeededOutboxEvent = paymentEventFactory.buildSucceededOutboxEvent(finalIntent, lastFour);
            outboxEventRepository.save(succeededOutboxEvent);

            return PaymentIntentResponse.fromEntity(finalIntent);
        } else {
            String reason = bankResponse != null ? bankResponse.declineReason() : "Bank authorization declined";
            log.warn("PaymentIntent {} declined by bank. Reason: {}", id, reason);
            intent.setStatus(PaymentStatus.FAILED);
            intent.setFailureReason(reason);

            PaymentIntent finalIntent = paymentIntentRepository.save(intent);

            OutboxEvent failedOutboxEvent = paymentEventFactory.buildFailedOutboxEvent(
                    finalIntent,
                    PaymentFailureCode.BANK_DECLINED,
                    reason
            );
            outboxEventRepository.save(failedOutboxEvent);

            return PaymentIntentResponse.fromEntity(finalIntent);
        }
    }

    @Transactional(readOnly = true)
    public PaymentIntentResponse getPaymentById(UUID id) {
        log.info("Fetching PaymentIntent by id: {}", id);
        PaymentIntent intent = paymentIntentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentIntent with id '" + id + "' not found"));
        return PaymentIntentResponse.fromEntity(intent);
    }

    @Transactional
    public PaymentIntentResponse cancelPayment(UUID id) {
        log.info("Cancelling PaymentIntent by id: {}", id);
        PaymentIntent intent = paymentIntentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("PaymentIntent with id '" + id + "' not found"));

        if (intent.getStatus() != PaymentStatus.CREATED) {
            throw new DomainRuleViolationException("Cannot cancel PaymentIntent in state: " + intent.getStatus());
        }

        intent.setStatus(PaymentStatus.CANCELLED);
        intent.setFailureReason("PaymentIntent was cancelled by merchant");
        PaymentIntent savedIntent = paymentIntentRepository.save(intent);

        OutboxEvent cancelledOutboxEvent = paymentEventFactory.buildFailedOutboxEvent(
                savedIntent,
                PaymentFailureCode.PAYMENT_CANCELLED,
                "PaymentIntent was cancelled by merchant"
        );
        outboxEventRepository.save(cancelledOutboxEvent);

        return PaymentIntentResponse.fromEntity(savedIntent);
    }

    private String extractLastFour(String maskedPan) {
        if (maskedPan != null && maskedPan.length() >= 4) {
            return maskedPan.substring(maskedPan.length() - 4);
        }
        return "****";
    }
}

