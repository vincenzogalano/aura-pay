package com.aurapay.orchestrator.service;

import com.aurapay.core.exception.DomainRuleViolationException;
import com.aurapay.core.exception.ResourceNotFoundException;
import com.aurapay.orchestrator.client.BankSimulatorClient;
import com.aurapay.orchestrator.client.VaultClient;
import com.aurapay.orchestrator.client.dto.BankAuthorizationRequest;
import com.aurapay.orchestrator.client.dto.BankAuthorizationResponse;
import com.aurapay.orchestrator.client.dto.VaultCardDetailsResponse;
import com.aurapay.orchestrator.domain.PaymentIntent;
import com.aurapay.orchestrator.domain.enums.PaymentStatus;
import com.aurapay.orchestrator.dto.request.ConfirmPaymentIntentRequest;
import com.aurapay.orchestrator.dto.request.CreatePaymentIntentRequest;
import com.aurapay.orchestrator.dto.response.PaymentIntentResponse;
import com.aurapay.orchestrator.repository.PaymentIntentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentOrchestrationServiceTest {

    @Mock
    private PaymentIntentRepository paymentIntentRepository;

    @Mock
    private VaultClient vaultClient;

    @Mock
    private BankSimulatorClient bankSimulatorClient;

    @InjectMocks
    private PaymentOrchestrationService paymentOrchestrationService;

    private UUID merchantId;
    private UUID intentId;
    private String validLuhnToken;

    @BeforeEach
    void setUp() {
        merchantId = UUID.randomUUID();
        intentId = UUID.randomUUID();
        validLuhnToken = "tok_4111111111111111"; // Valid Luhn card token
    }

    @Test
    @DisplayName("createPaymentIntent - Dovrebbe creare un PaymentIntent in stato CREATED")
    void createPaymentIntent_Success() {
        CreatePaymentIntentRequest request = new CreatePaymentIntentRequest(merchantId, 10000L, "EUR", "E-commerce purchase", true);

        given(paymentIntentRepository.save(any(PaymentIntent.class))).willAnswer(invocation -> {
            PaymentIntent arg = invocation.getArgument(0);
            arg.setId(intentId);
            arg.setCreatedAt(Instant.now());
            arg.setUpdatedAt(Instant.now());
            return arg;
        });

        PaymentIntentResponse response = paymentOrchestrationService.createPaymentIntent(request);

        assertNotNull(response);
        assertEquals(intentId, response.id());
        assertEquals(merchantId, response.merchantId());
        assertEquals(10000L, response.amountCents());
        assertEquals(PaymentStatus.CREATED, response.status());
        assertTrue(response.isTest());
    }

    @Test
    @DisplayName("confirmPayment - Happy Path Sincrono: Vault OK -> Bank Authorize OK -> Status SUCCEEDED")
    void confirmPayment_HappyPath_Success() {
        PaymentIntent intent = PaymentIntent.builder()
                .id(intentId)
                .merchantId(merchantId)
                .amountCents(10000L)
                .currency("EUR")
                .status(PaymentStatus.CREATED)
                .isTest(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ConfirmPaymentIntentRequest request = new ConfirmPaymentIntentRequest(validLuhnToken);

        given(paymentIntentRepository.findById(intentId)).willReturn(Optional.of(intent));
        given(paymentIntentRepository.save(any(PaymentIntent.class))).willAnswer(i -> i.getArgument(0));

        VaultCardDetailsResponse cardDetails = new VaultCardDetailsResponse(
                "4111111111111111", "John Doe", 12, 2028, "123", "411111******1111", "VISA", true
        );
        given(vaultClient.retrieveCardDetails(validLuhnToken)).willReturn(cardDetails);

        BankAuthorizationResponse bankResponse = BankAuthorizationResponse.approved("tx_bank_12345", "AUTH_987654");
        given(bankSimulatorClient.authorizePayment(any(BankAuthorizationRequest.class))).willReturn(bankResponse);

        PaymentIntentResponse response = paymentOrchestrationService.confirmPayment(intentId, request);

        assertEquals(PaymentStatus.SUCCEEDED, response.status());
        assertEquals("AUTH_987654", response.authorizationCode());
        assertEquals("tx_bank_12345", response.transactionId());
        assertNull(response.failureReason());
    }

    @Test
    @DisplayName("confirmPayment - Rifiuto Bancario: Vault OK -> Bank Declined -> Status FAILED")
    void confirmPayment_BankDeclined() {
        PaymentIntent intent = PaymentIntent.builder()
                .id(intentId)
                .merchantId(merchantId)
                .amountCents(10099L) // Magic amount 99 for insufficient funds
                .currency("EUR")
                .status(PaymentStatus.CREATED)
                .isTest(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        ConfirmPaymentIntentRequest request = new ConfirmPaymentIntentRequest(validLuhnToken);

        given(paymentIntentRepository.findById(intentId)).willReturn(Optional.of(intent));
        given(paymentIntentRepository.save(any(PaymentIntent.class))).willAnswer(i -> i.getArgument(0));

        VaultCardDetailsResponse cardDetails = new VaultCardDetailsResponse(
                "4111111111111111", "John Doe", 12, 2028, "123", "411111******1111", "VISA", true
        );
        given(vaultClient.retrieveCardDetails(validLuhnToken)).willReturn(cardDetails);

        BankAuthorizationResponse bankResponse = BankAuthorizationResponse.declined("51", "INSUFFICIENT_FUNDS");
        given(bankSimulatorClient.authorizePayment(any(BankAuthorizationRequest.class))).willReturn(bankResponse);

        PaymentIntentResponse response = paymentOrchestrationService.confirmPayment(intentId, request);

        assertEquals(PaymentStatus.FAILED, response.status());
        assertEquals("INSUFFICIENT_FUNDS", response.failureReason());
        assertNull(response.authorizationCode());
    }

    @Test
    @DisplayName("confirmPayment - Dovrebbe lanciare DomainRuleViolationException se lo stato non è CREATED")
    void confirmPayment_InvalidState() {
        PaymentIntent intent = PaymentIntent.builder()
                .id(intentId)
                .merchantId(merchantId)
                .amountCents(5000L)
                .currency("EUR")
                .status(PaymentStatus.SUCCEEDED)
                .isTest(true)
                .build();

        ConfirmPaymentIntentRequest request = new ConfirmPaymentIntentRequest(validLuhnToken);
        given(paymentIntentRepository.findById(intentId)).willReturn(Optional.of(intent));

        assertThrows(DomainRuleViolationException.class, () -> paymentOrchestrationService.confirmPayment(intentId, request));
    }

    @Test
    @DisplayName("cancelPayment - Dovrebbe annullare un PaymentIntent in stato CREATED")
    void cancelPayment_Success() {
        PaymentIntent intent = PaymentIntent.builder()
                .id(intentId)
                .merchantId(merchantId)
                .amountCents(5000L)
                .currency("EUR")
                .status(PaymentStatus.CREATED)
                .isTest(true)
                .build();

        given(paymentIntentRepository.findById(intentId)).willReturn(Optional.of(intent));
        given(paymentIntentRepository.save(any(PaymentIntent.class))).willAnswer(i -> i.getArgument(0));

        PaymentIntentResponse response = paymentOrchestrationService.cancelPayment(intentId);

        assertEquals(PaymentStatus.CANCELLED, response.status());
    }
}
