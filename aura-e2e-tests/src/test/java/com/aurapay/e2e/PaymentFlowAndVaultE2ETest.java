package com.aurapay.e2e;

import com.aurapay.core.exception.BusinessException;
import com.aurapay.orchestrator.client.BankSimulatorClient;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment_e2edb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.kafka.bootstrap-servers=localhost:9092",
        "spring.kafka.listener.auto-startup=false"
})
@Transactional
class PaymentFlowAndVaultE2ETest {

    @Autowired
    private VaultService vaultService;

    @Autowired
    private PaymentOrchestrationService paymentOrchestrationService;

    @MockitoBean
    private HashiCorpVaultClient vaultTransitClient;

    @MockitoBean
    private com.aurapay.orchestrator.client.VaultServiceClient orchestratorVaultClient;

    @MockitoBean
    private BankSimulatorClient bankSimulatorClient;

    @Test
    @DisplayName("E2E - Tokenizzazione carta valida restituisce un token temporaneo con brand e PAN mascherato")
    void e2e_cardTokenization_success() {
        given(vaultTransitClient.encrypt(any())).willReturn("vault:v1:encryptedCiphertextBase64");

        TokenizeRequest req = new TokenizeRequest(
                "4242424242424242", "Mario Rossi", 12, 2028, "123"
        );

        TokenResponse tokenResp = vaultService.tokenize(req, "Bearer pk_test_sampleKey123");

        assertThat(tokenResp).isNotNull();
        assertThat(tokenResp.token()).startsWith("tok_");
        assertThat(tokenResp.cardBrand().name()).isEqualTo("VISA");
        assertThat(tokenResp.maskedPan()).isEqualTo("424242******4242");
        assertThat(tokenResp.expiresAt()).isNotNull();
    }

    @Test
    @DisplayName("E2E - Tokenizzazione rifiutata per carta non valida (algoritmo Luhn)")
    void e2e_cardTokenization_invalidLuhn_throwsException() {
        TokenizeRequest req = new TokenizeRequest(
                "4532011111111112", "Mario Rossi", 12, 2028, "123" // Invalid Luhn checksum
        );

        assertThatThrownBy(() -> vaultService.tokenize(req, "Bearer pk_test_sampleKey123"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Luhn validation failed");
    }

    @Test
    @DisplayName("E2E - Flusso Pagamento completato con successo (CREATED -> PROCESSING -> SUCCEEDED)")
    void e2e_paymentFlow_success() {
        UUID merchantId = UUID.randomUUID();
        CreatePaymentIntentRequest createReq = new CreatePaymentIntentRequest(
                merchantId, 10000L, "EUR", "Subscription Payment", true
        );

        PaymentIntentResponse createdResp = paymentOrchestrationService.createPaymentIntent(createReq);
        assertThat(createdResp.status()).isEqualTo(PaymentStatus.CREATED);

        // Mock Vault detokenization
        VaultCardDetailsResponse cardDetails = new VaultCardDetailsResponse(
                "4242424242424242", "Mario Rossi", 12, 2028, "123", "424242******4242", "VISA", true
        );
        given(orchestratorVaultClient.retrieveCardDetails(any())).willReturn(cardDetails);

        // Mock Bank Simulator approval
        BankAuthorizationResponse bankResponse = BankAuthorizationResponse.approved("tx_bank_12345", "AUTH_98765");
        given(bankSimulatorClient.authorizePayment(any(BankAuthorizationRequest.class))).willReturn(bankResponse);

        // Confirm payment
        ConfirmPaymentIntentRequest confirmReq = new ConfirmPaymentIntentRequest("tok_4532011111111111");
        PaymentIntentResponse confirmedResp = paymentOrchestrationService.confirmPayment(createdResp.id(), confirmReq);

        assertThat(confirmedResp.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(confirmedResp.amountCents()).isEqualTo(10000L);
    }

    @Test
    @DisplayName("E2E - Flusso Pagamento rifiutato dalla banca per fondi insufficienti (Magic Rule *99)")
    void e2e_paymentFlow_insufficientFunds_failedStatus() {
        UUID merchantId = UUID.randomUUID();
        CreatePaymentIntentRequest createReq = new CreatePaymentIntentRequest(
                merchantId, 1099L, "EUR", "Insufficient Funds Test", true
        );

        PaymentIntentResponse createdResp = paymentOrchestrationService.createPaymentIntent(createReq);

        VaultCardDetailsResponse cardDetails = new VaultCardDetailsResponse(
                "4532011111111111", "Jane Doe", 11, 2029, "456", "453201******1111", "VISA", true
        );
        given(orchestratorVaultClient.retrieveCardDetails(any())).willReturn(cardDetails);

        // Mock Bank Simulator rejection for code 51 (Insufficient Funds)
        BankAuthorizationResponse bankResponse = BankAuthorizationResponse.declined("51", "INSUFFICIENT_FUNDS");
        given(bankSimulatorClient.authorizePayment(any(BankAuthorizationRequest.class))).willReturn(bankResponse);

        ConfirmPaymentIntentRequest confirmReq = new ConfirmPaymentIntentRequest("tok_4532011111111111");
        PaymentIntentResponse confirmedResp = paymentOrchestrationService.confirmPayment(createdResp.id(), confirmReq);

        assertThat(confirmedResp.status()).isEqualTo(PaymentStatus.FAILED);
    }
}
