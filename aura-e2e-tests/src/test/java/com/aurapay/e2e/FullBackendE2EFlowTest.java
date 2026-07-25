package com.aurapay.e2e;

import com.aurapay.core.events.PaymentSucceededEvent;
import com.aurapay.core.security.HmacUtils;
import com.aurapay.invoice.domain.Invoice;
import com.aurapay.invoice.domain.enums.InvoiceStatus;
import com.aurapay.invoice.dto.response.InvoiceDownloadUrlResponse;
import com.aurapay.invoice.publisher.InvoiceEventPublisher;
import com.aurapay.invoice.service.InvoiceService;
import com.aurapay.invoice.service.MinioStorageService;
import com.aurapay.ledger.dto.response.MerchantBalanceResponse;
import com.aurapay.ledger.service.LedgerEventPublisher;
import com.aurapay.ledger.service.LedgerService;
import com.aurapay.merchant.domain.enums.MerchantStatus;
import com.aurapay.merchant.dto.request.RegisterMerchantRequest;
import com.aurapay.merchant.dto.request.VerificationRequest;
import com.aurapay.merchant.dto.response.RegisterMerchantResponse;
import com.aurapay.merchant.dto.response.VerificationStatusResponse;
import com.aurapay.merchant.publisher.MerchantEventPublisher;
import com.aurapay.merchant.service.MerchantService;
import com.aurapay.orchestrator.client.BankSimulatorClient;
import com.aurapay.orchestrator.client.VaultServiceClient;
import com.aurapay.orchestrator.client.dto.BankAuthorizationRequest;
import com.aurapay.orchestrator.client.dto.BankAuthorizationResponse;
import com.aurapay.orchestrator.client.dto.VaultCardDetailsResponse;
import com.aurapay.orchestrator.domain.enums.PaymentStatus;
import com.aurapay.orchestrator.dto.request.ConfirmPaymentIntentRequest;
import com.aurapay.orchestrator.dto.request.CreatePaymentIntentRequest;
import com.aurapay.orchestrator.dto.response.PaymentIntentResponse;
import com.aurapay.orchestrator.service.PaymentOrchestrationService;
import com.aurapay.vault.client.HashiCorpVaultClient;
import com.aurapay.vault.dto.request.TokenizeRequest;
import com.aurapay.vault.dto.response.TokenResponse;
import com.aurapay.vault.service.VaultService;
import com.aurapay.webhook.dto.request.WebhookSubscriptionRequest;
import com.aurapay.webhook.dto.response.WebhookSubscriptionResponse;
import com.aurapay.webhook.publisher.WebhookEventPublisher;
import com.aurapay.webhook.service.WebhookService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:full_e2edb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
})
@Transactional
class FullBackendE2EFlowTest {

    @Autowired
    private MerchantService merchantService;

    @Autowired
    private VaultService vaultService;

    @Autowired
    private PaymentOrchestrationService paymentOrchestrationService;

    @Autowired
    private LedgerService ledgerService;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private WebhookService webhookService;

    // Mock external Publishers & Infrastructure Clients
    @MockitoBean
    private MerchantEventPublisher merchantEventPublisher;

    @MockitoBean
    private LedgerEventPublisher ledgerEventPublisher;

    @MockitoBean
    private InvoiceEventPublisher invoiceEventPublisher;

    @MockitoBean
    private WebhookEventPublisher webhookEventPublisher;

    @MockitoBean(name = "hashiCorpVaultClient")
    private HashiCorpVaultClient vaultTransitClient;

    @MockitoBean(name = "orchestratorVaultServiceClient")
    private VaultServiceClient orchestratorVaultClient;

    @MockitoBean
    private BankSimulatorClient bankSimulatorClient;

    @MockitoBean
    private MinioStorageService minioStorageService;

    @Test
    @DisplayName("E2E - Complete Backend Flow: Onboarding -> Payment -> Ledger -> Invoice -> Webhook")
    void e2e_fullIntegratedBackendFlow_success() {
        // 1. MERCHANT ONBOARDING
        RegisterMerchantRequest regReq = new RegisterMerchantRequest(
                "Full Flow Enterprises S.p.A.",
                "12345678903",
                "finance@fullflow.com"
        );

        RegisterMerchantResponse regResp = merchantService.registerMerchant(regReq);
        UUID merchantId = regResp.merchant().id();
        assertThat(regResp.merchant().status()).isEqualTo(MerchantStatus.PENDING_VERIFICATION);
        assertThat(regResp.testApiKeys()).hasSize(2);

        // 2. KYB VERIFICATION
        VerificationRequest verReq = new VerificationRequest(
                "MI-998877",
                "Corso Buenos Aires 45, Milano",
                "Mario Rossi"
        );
        VerificationStatusResponse verResp = merchantService.requestVerification(merchantId, verReq);
        assertThat(verResp.status()).isEqualTo(MerchantStatus.VERIFIED);
        assertThat(verResp.liveApiKeys()).hasSize(2);

        // 3. WEBHOOK SUBSCRIPTION
        WebhookSubscriptionRequest whReq = new WebhookSubscriptionRequest(
                merchantId,
                "https://fullflow.com/api/aurapay-webhooks",
                "whsec_fullflowSecret123",
                true,
                "payment.succeeded,invoice.generated"
        );
        WebhookSubscriptionResponse whResp = webhookService.createOrUpdateSubscription(whReq);
        assertThat(whResp.secretKey()).isEqualTo("whsec_fullflowSecret123");

        // 4. VAULT CARD TOKENIZATION
        given(vaultTransitClient.encrypt(any())).willReturn("vault:v1:encryptedDataSample");
        TokenizeRequest tokReq = new TokenizeRequest(
                "4242424242424242", "Mario Rossi", 10, 2028, "999"
        );
        TokenResponse tokResp = vaultService.tokenize(tokReq, "Bearer " + regResp.testApiKeys().get(0).keyPrefix());
        assertThat(tokResp.token()).startsWith("tok_");

        // 5. PAYMENT CREATION & CONFIRMATION
        CreatePaymentIntentRequest createIntentReq = new CreatePaymentIntentRequest(
                merchantId, 25000L, "EUR", "E2E Full Flow Order #1001", true
        );
        PaymentIntentResponse createdIntent = paymentOrchestrationService.createPaymentIntent(createIntentReq);
        assertThat(createdIntent.status()).isEqualTo(PaymentStatus.CREATED);

        VaultCardDetailsResponse cardDetails = new VaultCardDetailsResponse(
                "4242424242424242", "Mario Rossi", 10, 2028, "999", "424242******4242", "VISA", true
        );
        given(orchestratorVaultClient.retrieveCardDetails(any())).willReturn(cardDetails);
        given(bankSimulatorClient.authorizePayment(any(BankAuthorizationRequest.class)))
                .willReturn(BankAuthorizationResponse.approved("tx_bank_e2e_full", "AUTH_FULL_001"));

        ConfirmPaymentIntentRequest confirmReq = new ConfirmPaymentIntentRequest(tokResp.token());
        PaymentIntentResponse confirmedIntent = paymentOrchestrationService.confirmPayment(createdIntent.id(), confirmReq);
        assertThat(confirmedIntent.status()).isEqualTo(PaymentStatus.SUCCEEDED);

        // 6. LEDGER DOUBLE-ENTRY RECORDING
        PaymentSucceededEvent paySucceededEvent = new PaymentSucceededEvent(
                "evt_e2e_001",
                "payment.succeeded",
                Instant.now(),
                confirmedIntent.id().toString(),
                merchantId.toString(),
                25000L,
                500L,
                "EUR",
                "1111",
                "AUTH_FULL_001",
                true
        );
        ledgerService.recordPayment(paySucceededEvent);

        MerchantBalanceResponse balance = ledgerService.getMerchantBalance(merchantId.toString(), true);
        assertThat(balance.availableBalanceCents()).isEqualTo(24500L); // 25000 - 500

        // 7. INVOICE GENERATION & PRESIGNED URL
        Invoice invoice = invoiceService.processPaymentSucceeded(paySucceededEvent);
        assertThat(invoice.getInvoiceNumber()).startsWith("INV-");
        assertThat(invoice.getStatus()).isEqualTo(InvoiceStatus.GENERATED);

        InvoiceDownloadUrlResponse downloadUrl = invoiceService.generatePresignedDownloadUrl(invoice.getId());
        assertThat(downloadUrl.downloadUrl()).contains("signature=");

        // 8. WEBHOOK HMAC SIGNATURE VALIDATION
        String webhookPayload = String.format("{\"eventId\":\"%s\",\"eventType\":\"payment.succeeded\",\"amountCents\":25000}", paySucceededEvent.eventId());
        String calculatedSig = HmacUtils.calculateHmacSha256(webhookPayload, whResp.secretKey());
        boolean isSignatureValid = HmacUtils.verifyHmacSha256(webhookPayload, calculatedSig, whResp.secretKey());
        assertThat(isSignatureValid).isTrue();
    }
}
