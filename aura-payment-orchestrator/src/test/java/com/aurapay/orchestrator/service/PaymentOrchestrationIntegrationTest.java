package com.aurapay.orchestrator.service;

import com.aurapay.orchestrator.client.BankSimulatorClient;
import com.aurapay.orchestrator.client.VaultClient;
import com.aurapay.orchestrator.client.dto.BankAuthorizationRequest;
import com.aurapay.orchestrator.client.dto.BankAuthorizationResponse;
import com.aurapay.orchestrator.client.dto.VaultCardDetailsResponse;
import com.aurapay.orchestrator.domain.OutboxEvent;
import com.aurapay.orchestrator.domain.enums.PaymentStatus;
import com.aurapay.orchestrator.dto.request.ConfirmPaymentIntentRequest;
import com.aurapay.orchestrator.dto.request.CreatePaymentIntentRequest;
import com.aurapay.orchestrator.dto.response.PaymentIntentResponse;
import com.aurapay.orchestrator.repository.OutboxEventRepository;
import com.aurapay.orchestrator.repository.PaymentIntentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "SPRING_DATASOURCE_URL=jdbc:h2:mem:orchestratordb_it;DB_CLOSE_DELAY=-1;MODE=PostgreSQL"
})
class PaymentOrchestrationIntegrationTest {

    @Autowired
    private PaymentOrchestrationService paymentOrchestrationService;

    @Autowired
    private PaymentIntentRepository paymentIntentRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private VaultClient vaultClient;

    @MockitoBean
    private BankSimulatorClient bankSimulatorClient;

    @Test
    @DisplayName("Transactional Outbox - Dovrebbe persistere atomicamente PaymentIntent ed OutboxEvent durante il ciclo di vita")
    void testTransactionalOutboxPersistence() {
        UUID merchantId = UUID.randomUUID();
        CreatePaymentIntentRequest createReq = new CreatePaymentIntentRequest(merchantId, 5000L, "EUR", "Test Outbox", true);

        // 1. Create PaymentIntent -> saves 1 OutboxEvent (PAYMENT_INTENT_CREATED)
        PaymentIntentResponse createdResponse = paymentOrchestrationService.createPaymentIntent(createReq);
        assertNotNull(createdResponse);
        assertEquals(PaymentStatus.CREATED, createdResponse.status());

        List<OutboxEvent> outboxAfterCreate = outboxEventRepository.findAll();
        assertEquals(1, outboxAfterCreate.size());
        assertEquals("aura.paymentintent.created.v1", outboxAfterCreate.get(0).getEventType());
        assertEquals(createdResponse.id().toString(), outboxAfterCreate.get(0).getAggregateId());
        assertFalse(outboxAfterCreate.get(0).isProcessed());

        // Mocks for confirm
        VaultCardDetailsResponse cardDetails = new VaultCardDetailsResponse(
                "4532011111111111", "Jane Doe", 11, 2029, "456", "453201******1111", "VISA", true
        );
        given(vaultClient.retrieveCardDetails(any())).willReturn(cardDetails);

        BankAuthorizationResponse bankResponse = BankAuthorizationResponse.approved("tx_bank_999", "AUTH_111");
        given(bankSimulatorClient.authorizePayment(any(BankAuthorizationRequest.class))).willReturn(bankResponse);

        // 2. Confirm PaymentIntent -> saves 2 OutboxEvents (PROCESSING and SUCCEEDED)
        ConfirmPaymentIntentRequest confirmReq = new ConfirmPaymentIntentRequest("tok_4532011111111111");
        PaymentIntentResponse confirmedResponse = paymentOrchestrationService.confirmPayment(createdResponse.id(), confirmReq);

        assertEquals(PaymentStatus.SUCCEEDED, confirmedResponse.status());

        List<OutboxEvent> allOutboxEvents = outboxEventRepository.findAll();
        assertEquals(3, allOutboxEvents.size());

        assertTrue(allOutboxEvents.stream().anyMatch(e -> e.getEventType().equals("aura.paymentintent.created.v1")));
        assertTrue(allOutboxEvents.stream().anyMatch(e -> e.getEventType().equals("aura.payment.processing.v1")));
        assertTrue(allOutboxEvents.stream().anyMatch(e -> e.getEventType().equals("aura.payment.succeeded.v1")));
    }
}
